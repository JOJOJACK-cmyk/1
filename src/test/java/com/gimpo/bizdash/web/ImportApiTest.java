package com.gimpo.bizdash.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gimpo.bizdash.domain.BusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:import-api-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class ImportApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    BusinessRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("DB가 비어 있으면 샘플 데이터를 적재한다")
    void importsSampleIntoEmptyDatabase() throws Exception {
        mvc.perform(post("/api/import/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted").isNumber())
                .andExpect(jsonPath("$.outOfRegion").value(5))   // 샘플에 일부러 섞어 둔 타지역 행
                .andExpect(jsonPath("$.invalid").value(3));      // 인허가일자 없는 행

        assertThat(repository.count()).isGreaterThan(1000);
    }

    @Test
    @DisplayName("이미 데이터가 있으면 샘플을 섞지 않고 409로 거절한다")
    void rejectsWhenDataAlreadyExists() throws Exception {
        mvc.perform(post("/api/import/sample")).andExpect(status().isOk());
        long before = repository.count();

        mvc.perform(post("/api/import/sample"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isString());

        assertThat(repository.count()).isEqualTo(before);
    }
}
