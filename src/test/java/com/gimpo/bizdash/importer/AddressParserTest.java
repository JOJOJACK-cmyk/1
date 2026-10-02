package com.gimpo.bizdash.importer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressParserTest {

    @Test
    @DisplayName("지번주소에서 동을 뽑는다")
    void extractsDongFromJibunAddress() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 구래동 6871-2", "김포시")).isEqualTo("구래동");
        assertThat(AddressParser.extractDistrict("김포시 풍무동 123", "김포시")).isEqualTo("풍무동");
    }

    @Test
    @DisplayName("읍과 면도 뽑는다")
    void extractsEupAndMyeon() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 통진읍 마송리 12", "김포시")).isEqualTo("통진읍");
        assertThat(AddressParser.extractDistrict("경기도 김포시 대곶면 신안리 1", "김포시")).isEqualTo("대곶면");
    }

    @Test
    @DisplayName("괄호나 쉼표가 붙어도 뽑는다")
    void extractsWhenFollowedByParenOrComma() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 장기동 1000(상가)", "김포시")).isEqualTo("장기동");
        assertThat(AddressParser.extractDistrict("경기도 김포시 운양동, 2층", "김포시")).isEqualTo("운양동");
    }

    @Test
    @DisplayName("도로명주소는 끝의 괄호 안 법정동을 쓴다")
    void usesParenthesizedDongOfRoadAddress() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 김포한강9로76번길 37, 2층 201호 (구래동)", "김포시")).isEqualTo("구래동");
        assertThat(AddressParser.extractDistrict("경기도 김포시 금파로 1 (걸포동, 계양천 산책로 일대)", "김포시")).isEqualTo("걸포동");
        assertThat(AddressParser.extractDistrict("경기도 김포시 관순로 28, 106호 (사우동, 현중빌딩)", "김포시")).isEqualTo("사우동");
        // 읍·면은 도로명 주소에서도 시 바로 뒤에 온다
        assertThat(AddressParser.extractDistrict("경기도 김포시 고촌읍 신곡로3번길 43-26, 1층", "김포시")).isEqualTo("고촌읍");
    }

    @Test
    @DisplayName("괄호에 동이 없거나 다른 지역이면 미상")
    void returnsUnknownForRoadAddressOrOtherRegion() {
        assertThat(AddressParser.extractDistrict("경기도 김포시 김포한강9로 123", "김포시")).isEqualTo("미상");
        assertThat(AddressParser.extractDistrict("경기도 김포시 아라육로58번길 97 (3층,4층,5층일부)", "김포시")).isEqualTo("미상");
        assertThat(AddressParser.extractDistrict("서울특별시 강서구 화곡동 1-1", "김포시")).isEqualTo("미상");
        assertThat(AddressParser.extractDistrict(null, "김포시")).isEqualTo("미상");
        assertThat(AddressParser.extractDistrict("  ", "김포시")).isEqualTo("미상");
    }
}
