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
    void 김포_행만_적재하고_나머지는_센다() throws Exception {
        ImportResult result = importService.importFile(testCsv());

        assertThat(result.read()).isEqualTo(7);
        assertThat(result.inserted()).isEqualTo(5);
        assertThat(result.outOfRegion()).isEqualTo(1);   // 서울 강서구
        assertThat(result.invalid()).isEqualTo(1);       // 인허가일자 없음
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    void 필드가_제대로_변환된다() throws Exception {
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
    void 같은_파일을_다시_적재해도_중복되지_않는다() throws Exception {
        importService.importFile(testCsv());
        ImportResult second = importService.importFile(testCsv());

        assertThat(second.inserted()).isZero();
        assertThat(second.updated()).isEqualTo(5);
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    void CP949_파일도_읽는다(@TempDir Path dir) throws Exception {
        String text = Files.readString(testCsv());
        Path cp949 = dir.resolve("cp949.csv");
        Files.write(cp949, text.getBytes(Charset.forName("MS949")));

        ImportResult result = importService.importFile(cp949);

        assertThat(result.inserted()).isEqualTo(5);
        assertThat(repository.findByExternalIdIn(java.util.List.of("일반음식점:T-001")).get(0).getName())
                .isEqualTo("A식당");
    }

    @Test
    void BOM이_붙은_UTF8도_읽는다(@TempDir Path dir) throws Exception {
        String text = "﻿" + Files.readString(testCsv());
        Path bom = dir.resolve("bom.csv");
        Files.writeString(bom, text);

        assertThat(importService.importFile(bom).inserted()).isEqualTo(5);
    }

    @Test
    void 필수_컬럼이_없으면_이유를_알려준다(@TempDir Path dir) throws Exception {
        Path bad = dir.resolve("bad.csv");
        Files.writeString(bad, "이름,주소\n가게,김포시 사우동 1\n");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> importService.importFile(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사업장명");
    }

    @Test
    void 폴더를_주면_안의_csv를_모두_적재한다(@TempDir Path dir) throws Exception {
        Files.copy(testCsv(), dir.resolve("a.csv"));
        Files.writeString(dir.resolve("readme.txt"), "무시되어야 함");

        ImportResult result = importService.importPath(dir);

        assertThat(result.inserted()).isEqualTo(5);
    }
}
