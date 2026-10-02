package com.gimpo.bizdash.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.gimpo.bizdash.importer.DataFetcher.Outcome;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 실제 사이트 대신 로컬 가짜 서버로, 받을 수 있는 응답 형태를 모두 재현한다. */
class DataFetcherTest {

    private static final String CSV = "관리번호,사업장명,인허가일자,지번주소\n1,가게,2024-01-01,경기도 김포시 사우동 1\n";
    private static final Charset CP949 = Charset.forName("MS949");

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private String base;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/csv", ex -> respond(ex, 200, "text/csv", CSV.getBytes(CP949)));
        server.createContext("/zip", ex -> respond(ex, 200, "application/zip", zipOf("a.csv", CSV.getBytes(CP949))));
        server.createContext("/zip-two", ex -> respond(ex, 200, "application/zip",
                zipOf("a.csv", CSV.getBytes(CP949), "b.csv", CSV.getBytes(CP949))));
        server.createContext("/zip-no-csv", ex -> respond(ex, 200, "application/zip", zipOf("readme.txt", "x".getBytes())));
        server.createContext("/html", ex -> respond(ex, 200, "text/html", "<html>오류</html>".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/json", ex -> respond(ex, 200, "application/json", "{\"error\":1}".getBytes()));
        server.createContext("/empty", ex -> respond(ex, 200, "text/csv", new byte[0]));
        server.createContext("/missing", ex -> respond(ex, 404, "text/plain", "no".getBytes()));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void respond(com.sun.net.httpserver.HttpExchange ex, int code, String type, byte[] body) throws IOException {
        requests.incrementAndGet();
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(code, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            ex.getResponseBody().write(body);
        }
        ex.close();
    }

    private static byte[] zipOf(String name, byte[] content, Object... more) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, CP949)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content);
            zip.closeEntry();
            for (int i = 0; i < more.length; i += 2) {
                zip.putNextEntry(new ZipEntry((String) more[i]));
                zip.write((byte[]) more[i + 1]);
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private DataFetcher fetcher(int maxAgeDays) {
        return new DataFetcher(new FetchProperties(true, maxAgeDays, List.of()));
    }

    private Outcome fetch(DataFetcher f, String path, Path dir) {
        return f.fetch(new FetchProperties.Source("svc", base + path), dir);
    }

    @Test
    @DisplayName("CSV 응답을 name.csv 로 저장한다")
    void savesCsvResponse(@TempDir Path dir) throws IOException {
        assertThat(fetch(fetcher(7), "/csv", dir)).isEqualTo(Outcome.DOWNLOADED);
        assertThat(Files.readAllBytes(dir.resolve("svc.csv"))).isEqualTo(CSV.getBytes(CP949));
    }

    @Test
    @DisplayName("zip 응답은 풀어서 CSV 만 저장한다")
    void extractsCsvFromZip(@TempDir Path dir) throws IOException {
        assertThat(fetch(fetcher(7), "/zip", dir)).isEqualTo(Outcome.DOWNLOADED);
        assertThat(Files.readAllBytes(dir.resolve("svc.csv"))).isEqualTo(CSV.getBytes(CP949));
    }

    @Test
    @DisplayName("zip 안에 CSV 가 여러 개면 name_1.csv, name_2.csv 로 저장한다")
    void savesMultipleCsvFromZip(@TempDir Path dir) throws IOException {
        assertThat(fetch(fetcher(7), "/zip-two", dir)).isEqualTo(Outcome.DOWNLOADED);
        assertThat(dir.resolve("svc_1.csv")).exists();
        assertThat(dir.resolve("svc_2.csv")).exists();
    }

    @Test
    @DisplayName("HTML·JSON·빈 응답·zip 에 CSV 없음·404 는 저장하지 않고 실패로 처리한다")
    void rejectsNonCsvResponses(@TempDir Path dir) throws IOException {
        for (String path : List.of("/html", "/json", "/empty", "/zip-no-csv", "/missing")) {
            assertThat(fetch(fetcher(7), path, dir)).as(path).isEqualTo(Outcome.FAILED);
        }
        try (var files = Files.list(dir)) {
            assertThat(files.toList()).as("임시 파일 포함 아무것도 남지 않아야 함").isEmpty();
        }
    }

    @Test
    @DisplayName("접속할 수 없어도 예외 없이 실패로 처리한다")
    void doesNotThrowWhenServerIsUnreachable(@TempDir Path dir) {
        server.stop(0);
        assertThat(fetch(fetcher(7), "/csv", dir)).isEqualTo(Outcome.FAILED);
    }

    @Test
    @DisplayName("최근에 받은 파일이 있으면 다시 받지 않는다")
    void skipsFreshFile(@TempDir Path dir) throws IOException {
        fetch(fetcher(7), "/csv", dir);
        int before = requests.get();

        assertThat(fetch(fetcher(7), "/csv", dir)).isEqualTo(Outcome.SKIPPED_FRESH);
        assertThat(requests.get()).isEqualTo(before);
    }

    @Test
    @DisplayName("오래된 파일은 다시 받아 교체한다")
    void refetchesStaleFile(@TempDir Path dir) throws IOException {
        Path old = dir.resolve("svc.csv");
        Files.writeString(old, "old");
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(30, ChronoUnit.DAYS)));

        assertThat(fetch(fetcher(7), "/csv", dir)).isEqualTo(Outcome.DOWNLOADED);
        assertThat(Files.readAllBytes(old)).isEqualTo(CSV.getBytes(CP949));
    }

    @Test
    @DisplayName("다시 받기에 실패하면 이전 파일을 지우지 않는다")
    void keepsOldFileWhenRefetchFails(@TempDir Path dir) throws IOException {
        Path old = dir.resolve("svc.csv");
        Files.writeString(old, "old");
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(30, ChronoUnit.DAYS)));

        assertThat(fetch(fetcher(7), "/html", dir)).isEqualTo(Outcome.FAILED);
        assertThat(Files.readString(old)).isEqualTo("old");
    }

    @Test
    @DisplayName("꺼져 있으면 아무것도 하지 않는다")
    void doesNothingWhenDisabled(@TempDir Path dir) {
        var disabled = new DataFetcher(new FetchProperties(false, 7,
                List.of(new FetchProperties.Source("svc", base + "/csv"))));
        disabled.fetchAll(dir);
        assertThat(requests.get()).isZero();
    }
}
