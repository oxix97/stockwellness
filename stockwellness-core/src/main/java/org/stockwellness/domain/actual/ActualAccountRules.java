package org.stockwellness.domain.actual;

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

public final class ActualAccountRules {
    public static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
    private ActualAccountRules() { }

    public static String text(String value, int maxCodePoints, boolean optional) {
        if (value == null) {
            if (optional) return null;
            throw invalidInput();
        }
        int start = 0, end = value.length();
        while (start < end && whiteSpace(value.codePointAt(start)))
            start += Character.charCount(value.codePointAt(start));
        while (start < end && whiteSpace(value.codePointBefore(end)))
            end -= Character.charCount(value.codePointBefore(end));
        String normalized = value.substring(start, end);
        if (normalized.isEmpty()) {
            if (optional) return null;
            throw invalidInput();
        }
        if (normalized.codePointCount(0, normalized.length()) > maxCodePoints) throw invalidInput();
        return normalized;
    }

    private static boolean whiteSpace(int cp) {
        return cp >= 0x0009 && cp <= 0x000D || cp == 0x0020 || cp == 0x0085
                || cp == 0x00A0 || cp == 0x1680 || cp >= 0x2000 && cp <= 0x200A
                || cp == 0x2028 || cp == 0x2029 || cp == 0x202F || cp == 0x205F || cp == 0x3000;
    }

    public static void key(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,64}")) throw invalidInput();
    }

    public static long ifMatch(String token) {
        if (token == null || token.length() > 18 || !token.matches("\"(0|[1-9][0-9]*)\""))
            throw invalidInput();
        try {
            long version = Long.parseLong(token.substring(1, token.length() - 1));
            if (version > MAX_SAFE_INTEGER) throw invalidInput();
            return version;
        } catch (NumberFormatException ignored) { throw invalidInput(); }
    }

    public static long id(String token) {
        if (token == null || token.length() > 16 || !token.matches("[1-9][0-9]*")) throw invalidInput();
        try {
            long id = Long.parseLong(token);
            if (id > MAX_SAFE_INTEGER) throw invalidInput();
            return id;
        } catch (NumberFormatException ignored) { throw invalidInput(); }
    }

    public static GlobalException invalidInput() { return new GlobalException(ErrorCode.INVALID_INPUT_VALUE); }
}
