package com.gimpo.bizdash.importer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AddressParserTest {

    @Test
    void 지번주소에서_동을_뽑는다() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 구래동 6871-2", "김포시")).isEqualTo("구래동");
        assertThat(AddressParser.extractDistrict("김포시 풍무동 123", "김포시")).isEqualTo("풍무동");
    }

    @Test
    void 읍과_면도_뽑는다() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 통진읍 마송리 12", "김포시")).isEqualTo("통진읍");
        assertThat(AddressParser.extractDistrict("경기도 김포시 대곶면 신안리 1", "김포시")).isEqualTo("대곶면");
    }

    @Test
    void 괄호나_쉼표가_붙어도_뽑는다() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 장기동 1000(상가)", "김포시")).isEqualTo("장기동");
        assertThat(AddressParser.extractDistrict("경기도 김포시 운양동, 2층", "김포시")).isEqualTo("운양동");
    }

    @Test
    void 도로명주소나_다른_지역이면_미상() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 김포한강9로 123", "김포시")).isEqualTo("미상");
        assertThat(AddressParser.extractDistrict("서울특별시 강서구 화곡동 1-1", "김포시")).isEqualTo("미상");
        assertThat(AddressParser.extractDistrict(null, "김포시")).isEqualTo("미상");
        assertThat(AddressParser.extractDistrict("  ", "김포시")).isEqualTo("미상");
    }
}
