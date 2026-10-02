package com.gimpo.bizdash.importer;

public record ImportResult(long read, long inserted, long updated, long outOfRegion, long invalid) {

    public ImportResult plus(ImportResult o) {
        return new ImportResult(read + o.read, inserted + o.inserted, updated + o.updated,
                outOfRegion + o.outOfRegion, invalid + o.invalid);
    }

    @Override
    public String toString() {
        return "읽음 %d / 신규 %d / 갱신 %d / 지역 불일치 %d / 형식 오류 %d"
                .formatted(read, inserted, updated, outOfRegion, invalid);
    }
}
