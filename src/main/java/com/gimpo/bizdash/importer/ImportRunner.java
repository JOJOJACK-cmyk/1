package com.gimpo.bizdash.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 서버 시작 시 설정된 주소에서 CSV 를 내려받고(app.fetch), app.import.path(기본 import/)의 CSV 를 적재한다. 같은 파일을 다시 적재해도 중복되지 않는다. */
@Component
public class ImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportRunner.class);

    private final CsvImportService importService;
    private final ImportProperties props;
    private final DataFetcher fetcher;

    public ImportRunner(CsvImportService importService, ImportProperties props, DataFetcher fetcher) {
        this.importService = importService;
        this.props = props;
        this.fetcher = fetcher;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (props.path() == null || props.path().isBlank()) {
            return;
        }
        Path path = Path.of(props.path());
        fetcher.fetchAll(path); // 설정된 주소에서 CSV 를 내려받아 이 폴더에 저장 (꺼져 있거나 실패해도 아래 적재는 계속)
        if (!Files.exists(path)) {
            log.info("적재할 CSV 폴더가 없어 건너뜁니다: {} (CSV 를 넣고 다시 실행하면 자동 적재돼요)", path.toAbsolutePath());
            return;
        }
        log.info("CSV 적재 시작: {}", path.toAbsolutePath());
        ImportResult result = importService.importPath(path);
        log.info("CSV 적재 합계: {}", result);
    }
}
