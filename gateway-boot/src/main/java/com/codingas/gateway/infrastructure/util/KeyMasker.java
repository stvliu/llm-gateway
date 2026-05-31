package com.codingas.gateway.infrastructure.util;

/**
 * API Key 脱敏工具
 * 规则：保留前 6 位 + **** + 保留后 4 位
 * 长度不足 12 位时：仅显示前缀 + ****
 * 7-11 位时：保留前缀（全部已知字符）+ ****
 */
public final class KeyMasker {

    private static final int PREFIX_LEN = 6;
    private static final int SUFFIX_LEN = 4;
    private static final String MASK = "****";

    private KeyMasker() {}

    public static String mask(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (key.length() < 12) {
            return key + MASK;
        }
        String prefix = key.substring(0, Math.min(PREFIX_LEN, key.length()));
        String suffix = key.substring(key.length() - SUFFIX_LEN);
        return prefix + MASK + suffix;
    }
}
