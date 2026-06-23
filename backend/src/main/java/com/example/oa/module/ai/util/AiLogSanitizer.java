package com.example.oa.module.ai.util;

import java.util.regex.Pattern;

public final class AiLogSanitizer {
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/-]+=*");

    private AiLogSanitizer() {
    }

    public static String request(String value) {
        return truncate(redact(value), 4_000);
    }

    public static String response(String value) {
        return truncate(redact(value), 8_000);
    }

    public static String error(String value) {
        return truncate(redact(value), 500);
    }

    static String redact(String value) {
        if (value == null) return null;
        return BEARER.matcher(PHONE.matcher(EMAIL.matcher(value).replaceAll("[email]")).replaceAll("[phone]")).replaceAll("Bearer [token]");
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max) + "…[truncated]";
    }
}
