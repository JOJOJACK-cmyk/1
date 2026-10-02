package com.gimpo.bizdash.importer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 설정된 주소에서 CSV(또는 CSV가 든 zip)를 내려받아 적재 폴더에 저장한다.
 * 어떤 이유로 실패해도 예외를 던지지 않는다. (이미 받아 둔 파일로 서버는 정상 시작해야 하므로)
 * 응답이 CSV가 아니면(에러 페이지, JSON 등) 저장하지 않는다.
 */
@Component
public class DataFetcher {

    public enum Outcome { DOWNLOADED, SKIPPED_FRESH, FAILED }

    private static final Logger log = LoggerFactory.getLogger(DataFetcher.class);
    private static final Charset CP949 = Charset.forName("MS949");
    private static final int SNIFF_BYTES = 512;

    private final FetchProperties props;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public DataFetcher(FetchProperties props) {
        this.props = props;
    }

    public void fetchAll(Path dir) {
        if (!props.enabled() || props.sources().isEmpty()) {
            return;
        }
        for (FetchProperties.Source source : props.sources()) {
            fetch(source, dir);
        }
    }

    public Outcome fetch(FetchProperties.Source source, Path dir) {
        try {
            Files.createDirectories(dir);
            Instant newest = newestModified(dir, source.name());
            if (newest != null && newest.isAfter(Instant.now().minus(Duration.ofDays(props.maxAgeDays())))) {
                log.info("[{}] {}일 이내에 받은 파일이 있어 건너뜁니다.", source.name(), props.maxAgeDays());
                return Outcome.SKIPPED_FRESH;
            }
            log.info("[{}] 내려받는 중: {}", source.name(), source.url());
            HttpRequest request = HttpRequest.newBuilder(URI.create(source.url()))
                    .timeout(Duration.ofMinutes(15))
                    .header("User-Agent", "gimpo-biz-dashboard")
                    .GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    log.warn("[{}] 내려받기 실패: HTTP {}. 기존 파일이 있으면 그대로 씁니다.", source.name(), response.statusCode());
                    return Outcome.FAILED;
                }
                return save(new BufferedInputStream(body, 1 << 16), source, dir, response);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Outcome.FAILED;
        } catch (Exception e) {
            log.warn("[{}] 내려받기 실패: {}. 기존 파일이 있으면 그대로 씁니다.", source.name(), e.toString());
            return Outcome.FAILED;
        }
    }

    private Outcome save(BufferedInputStream in, FetchProperties.Source source, Path dir,
                         HttpResponse<?> response) throws IOException {
        in.mark(SNIFF_BYTES);
        byte[] head = in.readNBytes(SNIFF_BYTES);
        in.reset();

        List<Path> temps = new ArrayList<>();
        try {
            if (isZip(head)) {
                try (ZipInputStream zip = new ZipInputStream(in, CP949)) { // 항목 이름이 CP949 여도 읽히게
                    ZipEntry entry;
                    int index = 0;
                    while ((entry = zip.getNextEntry()) != null) {
                        if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".csv")) {
                            continue;
                        }
                        Path temp = Files.createTempFile(dir, ".download-" + source.name(), ".tmp");
                        temps.add(temp);
                        Files.copy(zip, temp, StandardCopyOption.REPLACE_EXISTING);
                        index++;
                    }
                }
                if (temps.isEmpty()) {
                    log.warn("[{}] zip 안에 CSV 가 없어요.", source.name());
                    return Outcome.FAILED;
                }
            } else {
                if (!looksLikeCsv(head)) {
                    log.warn("[{}] CSV 가 아닌 응답이라 저장하지 않았어요 (Content-Type: {}). 앞부분: {}",
                            source.name(), response.headers().firstValue("Content-Type").orElse("?"), preview(head));
                    return Outcome.FAILED;
                }
                Path temp = Files.createTempFile(dir, ".download-" + source.name(), ".tmp");
                temps.add(temp);
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }

            // 이전 파일을 지우고 새 파일로 교체. (임시 파일에 다 받은 뒤에 바꾸므로 중간에 실패해도 이전 파일이 남는다)
            deleteOld(dir, source.name());
            for (int i = 0; i < temps.size(); i++) {
                String fileName = temps.size() == 1 ? source.name() + ".csv" : source.name() + "_" + (i + 1) + ".csv";
                Files.move(temps.get(i), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            temps.clear();
            log.info("[{}] 저장 완료 ({}개 파일)", source.name(), Math.max(1, countSaved(dir, source.name())));
            return Outcome.DOWNLOADED;
        } finally {
            for (Path temp : temps) {
                Files.deleteIfExists(temp);
            }
        }
    }

    private static boolean isZip(byte[] head) {
        return head.length >= 4 && head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4;
    }

    /** 에러 페이지(HTML)나 JSON 이 아니고 비어 있지도 않으면 CSV 로 본다. (첫 글자가 < { [ 이면 CSV 가 아니다) */
    private static boolean looksLikeCsv(byte[] head) {
        int i = head.length >= 3 && head[0] == (byte) 0xEF && head[1] == (byte) 0xBB && head[2] == (byte) 0xBF ? 3 : 0;
        while (i < head.length && (head[i] == ' ' || head[i] == '\t' || head[i] == '\r' || head[i] == '\n')) {
            i++;
        }
        return i < head.length && head[i] != '<' && head[i] != '{' && head[i] != '[';
    }

    private static String preview(byte[] head) {
        String text = new String(head, 0, Math.min(head.length, 150), StandardCharsets.UTF_8);
        return text.replaceAll("\\s+", " ").strip();
    }

    private static Instant newestModified(Path dir, String name) throws IOException {
        Instant newest = null;
        try (var files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files.filter(p -> isFetched(p, name))::iterator) {
                Instant modified = Files.getLastModifiedTime(file).toInstant();
                if (newest == null || modified.isAfter(newest)) {
                    newest = modified;
                }
            }
        }
        return newest;
    }

    private static void deleteOld(Path dir, String name) throws IOException {
        try (var files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files.filter(p -> isFetched(p, name))::iterator) {
                Files.deleteIfExists(file);
            }
        }
    }

    private static long countSaved(Path dir, String name) throws IOException {
        try (var files = Files.list(dir)) {
            return files.filter(p -> isFetched(p, name)).count();
        }
    }

    /** name.csv 또는 name_숫자.csv (zip 에 CSV 가 여러 개인 경우) */
    private static boolean isFetched(Path file, String name) {
        String fileName = file.getFileName().toString();
        return fileName.equals(name + ".csv") || fileName.matches(java.util.regex.Pattern.quote(name) + "_\\d+\\.csv");
    }
}
