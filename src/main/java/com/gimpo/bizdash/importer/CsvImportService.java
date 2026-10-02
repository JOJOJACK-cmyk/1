package com.gimpo.bizdash.importer;

import com.gimpo.bizdash.domain.Business;
import com.gimpo.bizdash.domain.BusinessRepository;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
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
        return importBytes(Files.readAllBytes(file), file.getFileName().toString());
    }

    /** @param sourceName 로그에 찍을 이름 (파일명 등) */
    public ImportResult importBytes(byte[] bytes, String sourceName) throws IOException {
        String text = decode(bytes);
        long read = 0, inserted = 0, updated = 0, outOfRegion = 0, invalid = 0;

        try (CSVParser parser = FORMAT.parse(new StringReader(text))) {
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

    /** 공공데이터 CSV는 CP949(EUC-KR 확장)인 경우가 많다. UTF-8로 깨끗하게 읽히면 UTF-8, 아니면 CP949. */
    static String decode(byte[] bytes) {
        try {
            String utf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return stripBom(utf8);
        } catch (CharacterCodingException e) {
            return stripBom(new String(bytes, CP949));
        }
    }

    private static String stripBom(String s) {
        return !s.isEmpty() && s.charAt(0) == '﻿' ? s.substring(1) : s;
    }
}
