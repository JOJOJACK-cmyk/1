package com.gimpo.bizdash.importer;

import com.gimpo.bizdash.domain.Business;
import com.gimpo.bizdash.domain.BusinessStatus;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVRecord;

/**
 * 헤더 이름으로 컬럼을 찾아 한 행을 Business 후보로 바꾼다.
 * 공공데이터 파일마다 컬럼명이 조금씩 다르므로 후보 이름(별칭)을 순서대로 시도한다.
 */
final class CsvRowParser {

    private static final List<String> NAME = List.of("사업장명", "업소명", "상호명");
    private static final List<String> CATEGORY = List.of("업태구분명", "위생업태명", "업종명");
    private static final List<String> STATUS = List.of("영업상태명", "상세영업상태명");
    private static final List<String> LICENSED = List.of("인허가일자");
    private static final List<String> CLOSED = List.of("폐업일자");
    private static final List<String> JIBUN_ADDRESS = List.of("소재지전체주소", "지번주소");
    private static final List<String> ROAD_ADDRESS = List.of("도로명전체주소", "도로명주소");
    private static final List<String> MANAGEMENT_NO = List.of("관리번호");
    private static final List<String> SERVICE_NAME = List.of("개방서비스명");

    enum Skip { OUT_OF_REGION, INVALID }

    record Parsed(Business business, Skip skip) {
        static Parsed ok(Business b) { return new Parsed(b, null); }
        static Parsed skip(Skip s) { return new Parsed(null, s); }
    }

    private final Map<String, Integer> headerIndex;
    private final String regionKeyword;

    CsvRowParser(List<String> headers, String regionKeyword) {
        this.headerIndex = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerIndex.putIfAbsent(cleanHeader(headers.get(i)), i);
        }
        this.regionKeyword = regionKeyword;
        if (find(NAME) < 0) {
            throw new IllegalArgumentException("사업장명 컬럼을 찾지 못했습니다. 헤더: " + headers);
        }
        if (find(JIBUN_ADDRESS) < 0 && find(ROAD_ADDRESS) < 0) {
            throw new IllegalArgumentException("주소 컬럼을 찾지 못했습니다. 헤더: " + headers);
        }
        if (find(LICENSED) < 0) {
            throw new IllegalArgumentException("인허가일자 컬럼을 찾지 못했습니다. 헤더: " + headers);
        }
    }

    Parsed parse(CSVRecord r) {
        String jibun = get(r, JIBUN_ADDRESS);
        String road = get(r, ROAD_ADDRESS);
        // 지번 주소가 비어 도로명 주소만 있는 행도 있으니 둘 다 본다
        String address = inRegion(jibun) ? jibun : inRegion(road) ? road : null;
        if (address == null) {
            return Parsed.skip(Skip.OUT_OF_REGION);
        }

        String name = get(r, NAME);
        LocalDate licensed = DateParser.parse(get(r, LICENSED));
        if (name == null || licensed == null) {
            return Parsed.skip(Skip.INVALID);
        }
        LocalDate closed = DateParser.parse(get(r, CLOSED));

        String statusLabel = get(r, STATUS);
        BusinessStatus status = statusLabel != null
                ? BusinessStatus.fromLabel(statusLabel)
                : (closed != null ? BusinessStatus.CLOSED : BusinessStatus.OPEN);

        // 읍면동은 지번 주소에서만 나온다. 도로명 주소뿐이면 UNKNOWN.
        String district = AddressParser.extractDistrict(jibun, regionKeyword);
        if (AddressParser.UNKNOWN.equals(district)) {
            district = AddressParser.extractDistrict(road, regionKeyword);
        }

        Business b = new Business(externalId(r, name, address, licensed), name,
                CategoryNormalizer.normalize(get(r, CATEGORY)), status, licensed, closed, address, district);
        return Parsed.ok(b);
    }

    private boolean inRegion(String address) {
        return address != null && address.contains(regionKeyword);
    }

    private String externalId(CSVRecord r, String name, String address, LocalDate licensed) {
        String no = get(r, MANAGEMENT_NO);
        if (no != null) {
            String service = get(r, SERVICE_NAME);
            return (service == null ? "" : service + ":") + no;
        }
        // 관리번호가 없는 파일이면 사업장명+주소+인허가일로 대신한다
        return name + "|" + address + "|" + licensed;
    }

    private String get(CSVRecord r, List<String> aliases) {
        int idx = find(aliases);
        if (idx < 0 || idx >= r.size()) {
            return null;
        }
        String v = r.get(idx);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private int find(List<String> aliases) {
        for (String a : aliases) {
            Integer i = headerIndex.get(a);
            if (i != null) {
                return i;
            }
        }
        return -1;
    }

    private static String cleanHeader(String h) {
        return h == null ? "" : h.replace("﻿", "").trim();
    }
}
