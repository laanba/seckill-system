package com.seckill.api.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 Encryption Utility
 */
public class Md5Util {

    private static final String SALT = "seckill_salt_2026";

    /**
     * MD5 encryption with salt
     */
    public static String encrypt(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest((input + SALT).getBytes());
            return bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 encryption failed", e);
        }
    }

    /**
     * Simple MD5 encryption without salt
     */
    public static String simpleEncrypt(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes());
            return bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 encryption failed", e);
        }
    }

    /**
     * Convert bytes to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Verify password
     */
    public static boolean verify(String input, String encrypted) {
        return encrypt(input).equals(encrypted);
    }

    private Md5Util() {
    }
}
