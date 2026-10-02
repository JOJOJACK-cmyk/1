package com.gimpo.bizdash.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.gimpo.bizdash.domain.BusinessStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParsersTest {

    @Test
    @DisplayName("날짜는 여러 형식을 받는다")
    void parsesSeveralDateFormats() {
        LocalDate expected = LocalDate.of(2024, 3, 15);
        assertThat(DateParser.parse("2024-03-15")).isEqualTo(expected);
        assertThat(DateParser.parse("20240315")).isEqualTo(expected);
        assertThat(DateParser.parse("2024.03.15")).isEqualTo(expected);
        assertThat(DateParser.parse("2024-03-15 00:00:00.0")).isEqualTo(expected);
        assertThat(DateParser.parse(" 2024-3-5 ")).isEqualTo(LocalDate.of(2024, 3, 5));
    }

    @Test
    @DisplayName("비었거나 깨진 날짜는 null")
    void returnsNullForBlankOrBrokenDates() {
        assertThat(DateParser.parse(null)).isNull();
        assertThat(DateParser.parse("")).isNull();
        assertThat(DateParser.parse("0000-00-00")).isNull();
        assertThat(DateParser.parse("2024-13-40")).isNull();
        assertThat(DateParser.parse("미정")).isNull();
    }

    @Test
    @DisplayName("영업상태를 단순화한다")
    void simplifiesBusinessStatus() {
        assertThat(BusinessStatus.fromLabel("영업/정상")).isEqualTo(BusinessStatus.OPEN);
        assertThat(BusinessStatus.fromLabel("폐업")).isEqualTo(BusinessStatus.CLOSED);
        assertThat(BusinessStatus.fromLabel("휴업")).isEqualTo(BusinessStatus.SUSPENDED);
        assertThat(BusinessStatus.fromLabel("취소/말소/만료/정지/중지")).isEqualTo(BusinessStatus.OTHER);
        assertThat(BusinessStatus.fromLabel(null)).isEqualTo(BusinessStatus.OTHER);
    }

    @Test
    @DisplayName("업종을 표준화한다")
    void normalizesCategories() {
        assertThat(CategoryNormalizer.normalize("커피숍")).isEqualTo("카페");
        assertThat(CategoryNormalizer.normalize("까페")).isEqualTo("카페");
        assertThat(CategoryNormalizer.normalize(" 한식 ")).isEqualTo("한식");
        assertThat(CategoryNormalizer.normalize("")).isEqualTo("기타");
        assertThat(CategoryNormalizer.normalize(null)).isEqualTo("기타");
    }
}
