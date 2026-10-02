package com.gimpo.bizdash.importer;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 2024-03-15, 20240315, 2024.03.15, 2024-03-15 00:00:00 등을 LocalDate로. 빈 값/깨진 값은 null. */
public final class DateParser {

    private static final Pattern DATE = Pattern.compile("^(\\d{4})[-./]?(\\d{1,2})[-./]?(\\d{1,2})");

    private DateParser() {
    }

    public static LocalDate parse(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher m = DATE.matcher(raw.trim());
        if (!m.find()) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        } catch (DateTimeException e) {
            return null; // 0000-00-00 같은 값
        }
    }
}
