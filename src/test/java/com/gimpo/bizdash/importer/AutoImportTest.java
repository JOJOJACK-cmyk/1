package com.gimpo.bizdash.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.gimpo.bizdash.domain.BusinessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "app.import.path=src/test/resources/auto-import",
        "spring.datasource.url=jdbc:h2:mem:auto-import-test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class AutoImportTest {

    @Autowired
    BusinessRepository repository;

    @Test
    @DisplayName("서버가 시작되면 import 폴더의 CSV가 인수 없이 자동으로 적재된다")
    void importsFolderAutomaticallyOnStartup() {
        assertThat(repository.count()).isEqualTo(5);
    }
}
