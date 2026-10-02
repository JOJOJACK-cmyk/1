package com.gimpo.bizdash.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.gimpo.bizdash.domain.BusinessStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ParsersTest {

    @Test
    void 날짜는_여러_형식을_받는다() {
        LocalDate expected = LocalDate.of(2024, 3, 15);
        assertThat(DateParser.parse("2024-03-15")).isEqualTo(expected);
        assertThat(DateParser.parse("20240315")).isEqualTo(expected);
        assertThat(DateParser.parse("2024.03.15")).isEqualTo(expected);
        assertThat(DateParser.parse("2024-03-15 00:00:00.0")).isEqualTo(expected);
        assertThat(DateParser.parse(" 2024-3-5 ")).isEqualTo(LocalDate.of(2024, 3, 5));
    }

    @Test
    void 비었거나_깨진_날짜는_null() {
        assertThat(DateParser.parse(null)).isNull();
        assertThat(DateParser.parse("")).isNull();
        assertThat(DateParser.parse("0000-00-00")).isNull();
        assertThat(DateParser.parse("2024-13-40")).isNull();
        assertThat(DateParser.parse("미정")).isNull();
    }

    @Test
    void 영업상태를_단순화한다() {
        assertThat(BusinessStatus.fromLabel("영업/정상")).isEqualTo(BusinessStatus.OPEN);
        assertThat(BusinessStatus.fromLabel("폐업")).isEqualTo(BusinessStatus.CLOSED);
        assertThat(BusinessStatus.fromLabel("휴업")).isEqualTo(BusinessStatus.SUSPENDED);
        assertThat(BusinessStatus.fromLabel("취소/말소/만료/정지/중지")).isEqualTo(BusinessStatus.OTHER);
        assertThat(BusinessStatus.fromLabel(null)).isEqualTo(BusinessStatus.OTHER);
    }

    @Test
    void 업종을_표준화한다() {
        assertThat(CategoryNormalizer.normalize("커피숍")).isEqualTo("카페");
        assertThat(CategoryNormalizer.normalize("까페")).isEqualTo("카페");
        assertThat(CategoryNormalizer.normalize(" 한식 ")).isEqualTo("한식");
        assertThat(CategoryNormalizer.normalize("")).isEqualTo("기타");
        assertThat(CategoryNormalizer.normalize(null)).isEqualTo("기타");
    }
}
