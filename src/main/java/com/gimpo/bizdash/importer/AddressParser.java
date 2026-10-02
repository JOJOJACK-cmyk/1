package com.gimpo.bizdash.importer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 지번 주소에서 읍·면·동 이름을 뽑는다. 예) "경기도 김포시 구래동 6871-2" → "구래동" */
public final class AddressParser {

    public static final String UNKNOWN = "미상";

    private AddressParser() {
    }

    /** 도로명주소 끝의 괄호 안 법정동. 예) "김포한강9로76번길 37, 2층 (구래동, 연세프라자)" → 구래동 */
    private static final Pattern PAREN_DONG = Pattern.compile("\\(([가-힣0-9]+[동읍면])[,)]");

    /**
     * @param region "김포시" 처럼 시 이름. 이 뒤에 오는 첫 토큰이 읍/면/동으로 끝나야 인정한다.
     *               도로명 주소("김포시 김포한강2로 123")는 동이 없으므로, 끝의 괄호 안 법정동("(구래동)")을 찾고
     *               그것도 없으면 UNKNOWN
     */
    public static String extractDistrict(String address, String region) {
        if (address == null || address.isBlank()) {
            return UNKNOWN;
        }
        Matcher m = Pattern.compile(Pattern.quote(region) + "\\s+([가-힣0-9]+[동읍면])(?=[\\s,(0-9]|$)")
                .matcher(address);
        if (m.find()) {
            return m.group(1);
        }
        Matcher paren = PAREN_DONG.matcher(address);
        return paren.find() ? paren.group(1) : UNKNOWN;
    }
}
