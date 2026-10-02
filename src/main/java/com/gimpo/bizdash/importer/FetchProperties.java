package com.gimpo.bizdash.importer;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 서버 시작 때 CSV(또는 zip)를 내려받아 import/ 에 저장하는 설정.
 *
 * @param enabled    false 면 내려받지 않는다
 * @param maxAgeDays 받아 둔 파일이 이보다 새것이면 다시 받지 않는다
 * @param sources    내려받을 목록. name 이 저장 파일명(name.csv)이 된다
 */
@ConfigurationProperties(prefix = "app.fetch")
public record FetchProperties(boolean enabled, int maxAgeDays, List<Source> sources) {

    public record Source(String name, String url) {
    }

    public FetchProperties {
        if (maxAgeDays <= 0) {
            maxAgeDays = 7;
        }
        sources = sources == null ? List.of() : sources;
    }
}
