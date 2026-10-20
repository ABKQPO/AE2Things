package com.asdflj.ae2thing.util.enhancedsearch;

import java.util.List;

import com.asdflj.ae2thing.util.NeCharUtil;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class SearchTokens {

    private SearchTokens() {}

    public static boolean isExclusion(String token) {
        return token != null && token.length() > 1 && token.charAt(0) == '-';
    }

    public static List<String> split(String text) {
        final List<String> tokens = new ObjectArrayList<>();
        if (text == null) return tokens;
        for (String token : text.split(" +")) {
            if (!token.isEmpty()) tokens.add(token);
        }
        return tokens;
    }

    public static boolean matches(String token, String text) {
        if (token == null || token.isEmpty()) return true;
        if (text == null || text.isEmpty()) return false;
        if (isNumeric(token)) return containsBoundedDigits(token, text);
        return NeCharUtil.INSTANCE.contains(token, text);
    }

    public static boolean matchesAny(String token, String... texts) {
        if (token == null || token.isEmpty()) return true;
        for (String text : texts) {
            if (matches(token, text)) return true;
        }
        return false;
    }

    public static boolean matchesAll(List<String> tokens, String... texts) {
        if (tokens == null || tokens.isEmpty()) return true;
        for (String token : tokens) {
            if (token == null || token.isEmpty()) continue;
            if (isExclusion(token)) {
                if (matchesAny(token.substring(1), texts)) return false;
            } else if (!matchesAny(token, texts)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNumeric(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (isNotAsciiDigit(token.charAt(i))) return false;
        }
        return true;
    }

    private static boolean isNotAsciiDigit(char c) {
        return !(c >= '0' && c <= '9');
    }

    private static boolean containsBoundedDigits(String token, String text) {
        int from = 0;
        while (true) {
            final int idx = text.indexOf(token, from);
            if (idx < 0) return false;
            final int end = idx + token.length();
            if ((idx == 0 || isNotAsciiDigit(text.charAt(idx - 1)))
                && (end >= text.length() || isNotAsciiDigit(text.charAt(end)))) {
                return true;
            }
            from = idx + 1;
        }
    }
}
