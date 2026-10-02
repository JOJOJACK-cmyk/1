package com.gimpo.bizdash.web;

import com.gimpo.bizdash.domain.BusinessRepository;
import com.gimpo.bizdash.importer.CsvImportService;
import com.gimpo.bizdash.importer.ImportResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private static final String SAMPLE_RESOURCE = "sample/gimpo-sample.csv";

    private final BusinessRepository repository;
    private final CsvImportService importService;

    public ImportController(BusinessRepository repository, CsvImportService importService) {
        this.repository = repository;
        this.importService = importService;
    }

    /**
     * 가짜 샘플 데이터를 불러온다. 터미널·실행 인수 없이 화면의 버튼으로 시연용 데이터를 넣기 위한 것.
     * 실제 데이터에 가짜 데이터가 섞이지 않도록 DB가 비어 있을 때만 허용한다.
     */
    @PostMapping("/sample")
    public ResponseEntity<?> importSample() throws IOException {
        if (repository.count() > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 데이터가 있어서 샘플을 불러오지 않았어요."));
        }
        try (InputStream in = new ClassPathResource(SAMPLE_RESOURCE).getInputStream()) {
            ImportResult result = importService.importBytes(in.readAllBytes(), "gimpo-sample.csv");
            return ResponseEntity.ok(result);
        }
    }
}
