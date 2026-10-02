package com.gimpo.bizdash.domain;

public enum BusinessStatus {
    OPEN, SUSPENDED, CLOSED, OTHER;

    /** 인허가 데이터의 영업상태명(예: 영업/정상, 휴업, 폐업, 취소/말소/만료/정지/중지)을 단순화한다. */
    public static BusinessStatus fromLabel(String label) {
        if (label == null) {
            return OTHER;
        }
        if (label.contains("폐업")) {
            return CLOSED;
        }
        if (label.contains("휴업")) {
            return SUSPENDED;
        }
        if (label.contains("영업") || label.contains("정상")) {
            return OPEN;
        }
        return OTHER;
    }
}
