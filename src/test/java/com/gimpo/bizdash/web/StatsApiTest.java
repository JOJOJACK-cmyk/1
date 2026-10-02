package com.gimpo.bizdash.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gimpo.bizdash.domain.BusinessRepository;
import com.gimpo.bizdash.importer.CsvImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * src/test/resources/test-businesses.csv 의 정답은 손으로 계산해 둔 값이다.
 * 적재 후 기준일(asOf)은 데이터의 마지막 날짜인 2024-06-01.
 *
 * 개업: 2020 1(D) / 2022 1(A) / 2023 2(B,C) / 2024 1(E)
 * 폐업: 2024 3(A,C,D)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:stats-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class StatsApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    CsvImportService importService;
    @Autowired
    BusinessRepository repository;

    @BeforeEach
    void load() throws Exception {
        repository.deleteAll();
        importService.importFile(new ClassPathResource("test-businesses.csv").getFile().toPath());
    }

    @Test
    void meta는_건수와_기준일과_필터_후보를_준다() throws Exception {
        mvc.perform(get("/api/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.asOf").value("2024-06-01"))
                .andExpect(jsonPath("$.districts", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].category").value("한식"))
                .andExpect(jsonPath("$.categories[0].count").value(3));
    }

    @Test
    void 연도별_추이는_없는_해를_0으로_채운다() throws Exception {
        mvc.perform(get("/api/trend").param("years", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(5)))
                // 2020 ~ 2024
                .andExpect(jsonPath("$.points[0].year").value(2020))
                .andExpect(jsonPath("$.points[0].opened").value(1))
                .andExpect(jsonPath("$.points[1].year").value(2021))
                .andExpect(jsonPath("$.points[1].opened").value(0))
                .andExpect(jsonPath("$.points[4].year").value(2024))
                .andExpect(jsonPath("$.points[4].opened").value(1))
                .andExpect(jsonPath("$.points[4].closed").value(3))
                .andExpect(jsonPath("$.points[4].net").value(-2));
    }

    @Test
    void 동네와_업종으로_거를_수_있다() throws Exception {
        // 구래동 한식: 개업 A(2022) B(2023), 폐업 A(2024)
        mvc.perform(get("/api/trend").param("years", "3").param("district", "구래동").param("category", "한식"))
                .andExpect(jsonPath("$.points[0].year").value(2022))
                .andExpect(jsonPath("$.points[0].opened").value(1))
                .andExpect(jsonPath("$.points[1].opened").value(1))
                .andExpect(jsonPath("$.points[2].closed").value(1));
    }

    @Test
    void 업종별_영업기간은_폐업한_곳_기준이다() throws Exception {
        // 한식: A 2.0년, D 4.0년 → 평균·중앙값 3.0 / 카페: C 1.0년
        mvc.perform(get("/api/survival").param("minSample", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].category").value("한식"))
                .andExpect(jsonPath("$[0].closedCount").value(2))
                .andExpect(jsonPath("$[0].avgYears").value(3.0))
                .andExpect(jsonPath("$[0].medianYears").value(3.0))
                .andExpect(jsonPath("$[1].category").value("카페"))
                .andExpect(jsonPath("$[1].avgYears").value(1.0));
    }

    @Test
    void 표본이_적은_업종은_영업기간에서_뺀다() throws Exception {
        mvc.perform(get("/api/survival").param("minSample", "2"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("한식"));
    }

    @Test
    void 최근_12개월_랭킹은_데이터_마지막_날짜를_기준으로_한다() throws Exception {
        // 기간: 2023-06-01 초과 ~ 2024-06-01 이하
        mvc.perform(get("/api/ranking").param("months", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value("2024-06-01"))
                .andExpect(jsonPath("$.from").value("2023-06-01"))
                .andExpect(jsonPath("$.openedTotal").value(1))
                .andExpect(jsonPath("$.closedTotal").value(3))
                .andExpect(jsonPath("$.opened", hasSize(1)))
                .andExpect(jsonPath("$.opened[0].category").value("카페"))
                .andExpect(jsonPath("$.closed", hasSize(2)))
                .andExpect(jsonPath("$.closed[0].category").value("한식"))
                .andExpect(jsonPath("$.closed[0].count").value(2));
    }

    @Test
    void 읍면동별_개폐업() throws Exception {
        mvc.perform(get("/api/districts").param("months", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(2)))
                .andExpect(jsonPath("$.rows[0].district").value("구래동"))
                .andExpect(jsonPath("$.rows[0].opened").value(0))
                .andExpect(jsonPath("$.rows[0].closed").value(2))
                .andExpect(jsonPath("$.rows[0].net").value(-2))
                .andExpect(jsonPath("$.rows[1].district").value("사우동"))
                .andExpect(jsonPath("$.rows[1].opened").value(1))
                .andExpect(jsonPath("$.rows[1].closed").value(1));
    }

    @Test
    void 데이터가_없어도_빈_응답을_준다() throws Exception {
        repository.deleteAll();
        mvc.perform(get("/api/trend")).andExpect(status().isOk()).andExpect(jsonPath("$.points", hasSize(0)));
        mvc.perform(get("/api/ranking")).andExpect(status().isOk()).andExpect(jsonPath("$.opened", hasSize(0)));
        mvc.perform(get("/api/meta")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
}
