package com.pot.zing.framework.common.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * @author: Pot
 * @created: 2025/3/31 23:46
 * @description: 安全工具类
 */
@Slf4j
public class SecurityUtils {

    public static String hashHex(String input) {
        return hashHex(input, "MD5");
    }

    public static String hashHex(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withLowerCase().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm, e);
        }
    }
}
