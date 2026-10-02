package com.gimpo.bizdash.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.gimpo.bizdash.domain.Business;
import com.gimpo.bizdash.domain.BusinessRepository;
import com.gimpo.bizdash.domain.BusinessStatus;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:import-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class CsvImportServiceTest {

    @Autowired
    CsvImportService importService;
    @Autowired
    BusinessRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private Path testCsv() throws Exception {
        return new ClassPathResource("test-businesses.csv").getFile().toPath();
    }

    @Test
    @DisplayName("김포 행만 적재하고 나머지는 센다")
    void importsOnlyRegionRowsAndCountsTheRest() throws Exception {
        ImportResult result = importService.importFile(testCsv());

        assertThat(result.read()).isEqualTo(7);
        assertThat(result.inserted()).isEqualTo(5);
        assertThat(result.outOfRegion()).isEqualTo(1);   // 서울 강서구
        assertThat(result.invalid()).isEqualTo(1);       // 인허가일자 없음
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("필드가 제대로 변환된다")
    void convertsFields() throws Exception {
        importService.importFile(testCsv());

        Business a = repository.findByExternalIdIn(java.util.List.of("일반음식점:T-001")).get(0);
        assertThat(a.getName()).isEqualTo("A식당");
        assertThat(a.getDistrict()).isEqualTo("구래동");
        assertThat(a.getCategory()).isEqualTo("한식");
        assertThat(a.getStatus()).isEqualTo(BusinessStatus.CLOSED);
        assertThat(a.getLicensedDate()).isEqualTo(LocalDate.of(2022, 3, 1));
        assertThat(a.getClosedDate()).isEqualTo(LocalDate.of(2024, 3, 1));

        Business e = repository.findByExternalIdIn(java.util.List.of("휴게음식점:T-005")).get(0);
        assertThat(e.getCategory()).isEqualTo("카페");   // 까페 → 카페
        assertThat(e.getStatus()).isEqualTo(BusinessStatus.OPEN);
        assertThat(e.getClosedDate()).isNull();
    }

    @Test
    @DisplayName("같은 파일을 다시 적재해도 중복되지 않는다")
    void reimportDoesNotDuplicate() throws Exception {
        importService.importFile(testCsv());
        ImportResult second = importService.importFile(testCsv());

        assertThat(second.inserted()).isZero();
        assertThat(second.updated()).isEqualTo(5);
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("CP949 파일도 읽는다")
    void readsCp949File(@TempDir Path dir) throws Exception {
        String text = Files.readString(testCsv());
        Path cp949 = dir.resolve("cp949.csv");
        Files.write(cp949, text.getBytes(Charset.forName("MS949")));

        ImportResult result = importService.importFile(cp949);

        assertThat(result.inserted()).isEqualTo(5);
        assertThat(repository.findByExternalIdIn(java.util.List.of("일반음식점:T-001")).get(0).getName())
                .isEqualTo("A식당");
    }

    @Test
    @DisplayName("BOM이 붙은 UTF8도 읽는다")
    void readsUtf8WithBom(@TempDir Path dir) throws Exception {
        String text = "﻿" + Files.readString(testCsv());
        Path bom = dir.resolve("bom.csv");
        Files.writeString(bom, text);

        assertThat(importService.importFile(bom).inserted()).isEqualTo(5);
    }

    @Test
    @DisplayName("필수 컬럼이 없으면 이유를 알려준다")
    void explainsMissingRequiredColumn(@TempDir Path dir) throws Exception {
        Path bad = dir.resolve("bad.csv");
        Files.writeString(bad, "이름,주소\n가게,김포시 사우동 1\n");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> importService.importFile(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사업장명");
    }

    @Test
    @DisplayName("폴더를 주면 안의 csv를 모두 적재한다")
    void importsAllCsvFilesInDirectory(@TempDir Path dir) throws Exception {
        Files.copy(testCsv(), dir.resolve("a.csv"));
        Files.writeString(dir.resolve("readme.txt"), "무시되어야 함");

        ImportResult result = importService.importPath(dir);

        assertThat(result.inserted()).isEqualTo(5);
    }
}
