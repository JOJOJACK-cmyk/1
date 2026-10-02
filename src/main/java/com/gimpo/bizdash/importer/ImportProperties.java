package com.gimpo.bizdash.importer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param path          시작 시 적재할 CSV 파일 또는 폴더(폴더면 *.csv 전부). 비어 있으면 적재하지 않는다.
 * @param regionKeyword 주소에 이 문자열이 있는 행만 적재한다. 전국 파일을 받아도 김포만 남기기 위함.
 * @param batchSize     DB에 한 번에 저장할 행 수
 */
@ConfigurationProperties(prefix = "app.import")
public record ImportProperties(String path, String regionKeyword, int batchSize) {

    public ImportProperties {
        if (regionKeyword == null || regionKeyword.isBlank()) {
            regionKeyword = "김포시";
        }
        if (batchSize <= 0) {
            batchSize = 500;
        }
    }
}
