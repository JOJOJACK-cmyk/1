package com.gimpo.bizdash.importer;

import com.gimpo.bizdash.domain.Business;
import com.gimpo.bizdash.domain.BusinessRepository;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private static final Charset CP949 = Charset.forName("MS949");
    private static final int SNIFF_BYTES = 64 * 1024;

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setAllowMissingColumnNames(true)
            .get();

    private final BusinessRepository repository;
    private final TransactionTemplate tx;
    private final ImportProperties props;

    public CsvImportService(BusinessRepository repository, TransactionTemplate tx, ImportProperties props) {
        this.repository = repository;
        this.tx = tx;
        this.props = props;
    }

    /** 파일이면 그 파일을, 폴더면 안의 *.csv 전부를 적재한다. */
    public ImportResult importPath(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            return importFile(path);
        }
        List<Path> files;
        try (var stream = Files.list(path)) {
            files = stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv")).sorted().toList();
        }
        ImportResult total = new ImportResult(0, 0, 0, 0, 0);
        for (Path file : files) {
            total = total.plus(importFile(file));
        }
        return total;
    }

    public ImportResult importFile(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return importStream(in, file.getFileName().toString());
        }
    }

    public ImportResult importBytes(byte[] bytes, String sourceName) throws IOException {
        return importStream(new ByteArrayInputStream(bytes), sourceName);
    }

    /**
     * 파일 전체를 메모리에 올리지 않고 한 줄씩 읽어 적재한다. (전국 단위 수백 MB 파일도 처리)
     *
     * @param sourceName 로그에 찍을 이름 (파일명 등)
     */
    public ImportResult importStream(InputStream raw, String sourceName) throws IOException {
        BufferedInputStream in = new BufferedInputStream(raw, SNIFF_BYTES);
        Charset charset = sniffCharset(in);
        long read = 0, inserted = 0, updated = 0, outOfRegion = 0, invalid = 0;

        try (Reader reader = new InputStreamReader(in, charset); CSVParser parser = FORMAT.parse(reader)) {
            CsvRowParser rowParser = new CsvRowParser(parser.getHeaderNames(), props.regionKeyword());
            Map<String, Business> batch = new LinkedHashMap<>();

            for (CSVRecord record : parser) {
                read++;
                CsvRowParser.Parsed parsed = rowParser.parse(record);
                if (parsed.skip() == CsvRowParser.Skip.OUT_OF_REGION) {
                    outOfRegion++;
                } else if (parsed.skip() == CsvRowParser.Skip.INVALID) {
                    invalid++;
                } else {
                    // 같은 파일 안에서 키가 겹치면 뒤의 행이 이긴다
                    batch.put(parsed.business().getExternalId(), parsed.business());
                }
                if (batch.size() >= props.batchSize()) {
                    int[] r = flush(batch);
                    inserted += r[0];
                    updated += r[1];
                    batch.clear();
                }
            }
            int[] r = flush(batch);
            inserted += r[0];
            updated += r[1];
        }

        ImportResult result = new ImportResult(read, inserted, updated, outOfRegion, invalid);
        log.info("{} 적재 완료: {}", sourceName, result);
        return result;
    }

    /** @return {신규 건수, 갱신 건수} */
    private int[] flush(Map<String, Business> batch) {
        if (batch.isEmpty()) {
            return new int[]{0, 0};
        }
        return tx.execute(status -> {
            Map<String, Business> existing = repository.findByExternalIdIn(batch.keySet()).stream()
                    .collect(Collectors.toMap(Business::getExternalId, Function.identity()));
            List<Business> toInsert = new ArrayList<>();
            int updatedCount = 0;
            for (Business incoming : batch.values()) {
                Business found = existing.get(incoming.getExternalId());
                if (found == null) {
                    toInsert.add(incoming);
                } else {
                    found.update(incoming.getName(), incoming.getCategory(), incoming.getStatus(),
                            incoming.getLicensedDate(), incoming.getClosedDate(),
                            incoming.getAddress(), incoming.getDistrict());
                    updatedCount++;
                }
            }
            repository.saveAll(toInsert);
            return new int[]{toInsert.size(), updatedCount};
        });
    }

    /**
     * 공공데이터 CSV는 CP949(EUC-KR 확장)인 경우가 많다. 파일 앞부분이 UTF-8로 깨끗하게 읽히면 UTF-8, 아니면 CP949.
     * 헤더가 한글이라 앞부분만으로 충분히 판별된다. UTF-8 BOM 은 여기서 건너뛴다.
     */
    static Charset sniffCharset(BufferedInputStream in) throws IOException {
        in.mark(SNIFF_BYTES);
        byte[] head = in.readNBytes(SNIFF_BYTES);
        in.reset();
        if (head.length >= 3 && head[0] == (byte) 0xEF && head[1] == (byte) 0xBB && head[2] == (byte) 0xBF) {
            in.skipNBytes(3);
            return StandardCharsets.UTF_8;
        }
        boolean wholeFile = head.length < SNIFF_BYTES; // 잘린 앞부분이면 끝의 불완전한 글자는 오류로 보지 않는다
        CoderResult result = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(head), CharBuffer.allocate(head.length), wholeFile);
        return result.isError() ? CP949 : StandardCharsets.UTF_8;
    }
}
