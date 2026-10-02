package com.gimpo.bizdash.importer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 지번 주소에서 읍·면·동 이름을 뽑는다. 예) "경기도 김포시 구래동 6871-2" → "구래동" */
public final class AddressParser {

    public static final String UNKNOWN = "미상";

    private AddressParser() {
    }

    /**
     * @param region "김포시" 처럼 시 이름. 이 뒤에 오는 첫 토큰이 읍/면/동으로 끝나야 인정한다.
     *               (도로명 주소 "김포시 김포한강2로 123" 은 동이 없으므로 UNKNOWN)
     */
    public static String extractDistrict(String address, String region) {
        if (address == null || address.isBlank()) {
            return UNKNOWN;
        }
        Matcher m = Pattern.compile(Pattern.quote(region) + "\\s+([가-힣0-9]+[동읍면])(?=[\\s,(0-9]|$)")
                .matcher(address);
        return m.find() ? m.group(1) : UNKNOWN;
    }
}
