package com.codingas.gateway.infrastructure.util;

/**
 * API Key 脱敏工具
 * 规则：
 * - 长度 <= 8：保留前 4 位 + ****（避免短 key 脱敏后反而变长）
 * - 长度 > 8：保留前 6 位 + **** + 保留后 4 位
 */
public final class KeyMasker {

    private static final int SHORT_KEY_PREFIX_LEN = 4;
    private static final int PREFIX_LEN = 6;
    private static final int SUFFIX_LEN = 4;
    private static final String MASK = "****";
    private static final int SHORT_KEY_THRESHOLD = 8;

    private KeyMasker() {}

    public static String mask(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        // 短 key（<=8位）：只显示前缀 + ****，避免脱敏后比原始 key 还长
        if (key.length() <= SHORT_KEY_THRESHOLD) {
            int prefixLen = Math.min(SHORT_KEY_PREFIX_LEN, key.length());
            return key.substring(0, prefixLen) + MASK;
        }
        // 长 key：前缀 + **** + 后缀
        return key.substring(0, PREFIX_LEN) + MASK + key.substring(key.length() - SUFFIX_LEN);
    }
}
