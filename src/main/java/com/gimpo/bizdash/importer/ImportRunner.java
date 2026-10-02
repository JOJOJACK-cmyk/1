package com.gimpo.bizdash.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 서버 시작 시 app.import.path(기본 import/)의 CSV 를 적재한다. 같은 파일을 다시 적재해도 중복되지 않는다. */
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
            log.info("적재할 CSV 폴더가 없어 건너뜁니다: {} (CSV 를 넣고 다시 실행하면 자동 적재돼요)", path.toAbsolutePath());
            return;
        }
        log.info("CSV 적재 시작: {}", path.toAbsolutePath());
        ImportResult result = importService.importPath(path);
        log.info("CSV 적재 합계: {}", result);
    }
}
