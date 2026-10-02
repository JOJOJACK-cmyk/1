package com.gimpo.bizdash.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** app.import.path 가 지정되면 서버 시작 시 CSV를 적재한다. 예) ./gradlew bootRun --args='--app.import.path=import/' */
@Component
public class ImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportRunner.class);

    private final CsvImportService importService;
    private final ImportProperties props;

    public ImportRunner(CsvImportService importService, ImportProperties props) {
        this.importService = importService;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (props.path() == null || props.path().isBlank()) {
            return;
        }
        Path path = Path.of(props.path());
        if (!Files.exists(path)) {
            log.warn("app.import.path 를 찾을 수 없습니다: {}", path.toAbsolutePath());
            return;
        }
        log.info("CSV 적재 시작: {}", path.toAbsolutePath());
        ImportResult result = importService.importPath(path);
        log.info("CSV 적재 합계: {}", result);
    }
}
