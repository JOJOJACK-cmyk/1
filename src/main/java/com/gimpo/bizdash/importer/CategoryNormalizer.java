package com.gimpo.bizdash.importer;

import java.util.Map;

/**
 * 업태구분명 표준화. 비슷한 업종을 하나로 묶어 그래프가 쪼개지지 않게 한다.
 * 아래 매핑은 출발점일 뿐이니, 실제 데이터를 적재한 뒤 GET /api/meta 의 categories 를 보고 조정할 것.
 */
public final class CategoryNormalizer {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("까페", "카페"),
            Map.entry("커피숍", "카페"),
            Map.entry("다방", "카페"),
            Map.entry("전통찻집", "카페"),
            Map.entry("호프/통닭", "치킨/호프"),
            Map.entry("통닭(치킨)", "치킨/호프"),
            Map.entry("김밥(도시락)", "분식"),
            Map.entry("정종/대포집/소주방", "주점"),
            Map.entry("감성주점", "주점"),
            Map.entry("제과점영업", "제과점"));

    private CategoryNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "기타";
        }
        String trimmed = raw.trim();
        return ALIASES.getOrDefault(trimmed, trimmed);
    }
}
