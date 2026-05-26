package com.master.finance.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

public class TotpUtil {

    private static final int SECRET_SIZE = 20;
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP = 30;
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    public static String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        new SecureRandom().nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer)
                .replaceAll("=+$", "")
                .replaceAll("[^A-Za-z0-9]", "");
    }

    public static String getProvisioningUri(String secret, String accountName, String issuer) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
            issuer, accountName, secret, issuer, CODE_DIGITS, TIME_STEP
        );
    }

    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) return false;
        try {
            long counter = System.currentTimeMillis() / 1000 / TIME_STEP;
            String expected = generateTOTP(secret, counter);
            String expectedBefore = generateTOTP(secret, counter - 1);
            String expectedAfter = generateTOTP(secret, counter + 1);
            return code.equals(expected) || code.equals(expectedBefore) || code.equals(expectedAfter);
        } catch (Exception e) {
            return false;
        }
    }

    private static String generateTOTP(String secret, long counter) throws Exception {
        byte[] data = new byte[8];
        long value = counter;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(base32Decode(secret), HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format(Locale.US, "%0" + CODE_DIGITS + "d", otp);
    }

    private static byte[] base32Decode(String secret) {
        StringBuilder cleaned = new StringBuilder();
        for (char c : secret.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'Z' || c >= '2' && c <= '7') {
                cleaned.append(c);
            }
        }
        String base32 = cleaned.toString();
        byte[] bytes = new byte[base32.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (int i = 0; i < base32.length(); i++) {
            char c = base32.charAt(i);
            int val;
            if (c >= 'A' && c <= 'Z') val = c - 'A';
            else val = c - '2' + 26;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        if (index < bytes.length) {
            byte[] trimmed = new byte[index];
            System.arraycopy(bytes, 0, trimmed, 0, index);
            return trimmed;
        }
        return bytes;
    }
}
