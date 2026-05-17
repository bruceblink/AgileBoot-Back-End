package app.keystone.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Shared secret:v1 AES-256-GCM decryptor for deployment secrets.
 */
public final class SecretValueDecryptor {

    public static final String SECRET_V1_PREFIX = "secret:v1:aes-256-gcm";

    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_NONCE_BYTES = 12;

    private SecretValueDecryptor() {
    }

    public static boolean isSecretV1(String value) {
        return value != null && value.trim().startsWith(SECRET_V1_PREFIX);
    }

    public static String decryptSecretV1(String encryptedValue, String encryptKey) throws Exception {
        String[] parts = encryptedValue.trim().split(":");
        if (parts.length != 5
            || !"secret".equals(parts[0])
            || !"v1".equals(parts[1])
            || !"aes-256-gcm".equals(parts[2])) {
            throw new IllegalArgumentException(
                "Secret must use format secret:v1:aes-256-gcm:<nonce_base64>:<ciphertext_base64>");
        }

        byte[] nonce = Base64.getDecoder().decode(parts[3]);
        if (nonce.length != GCM_NONCE_BYTES) {
            throw new IllegalArgumentException("AES-GCM nonce must be 12 bytes");
        }

        byte[] ciphertextAndTag = Base64.getDecoder().decode(parts[4]);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decodeSecretKey(encryptKey), "AES"),
            new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] decrypted = cipher.doFinal(ciphertextAndTag);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static byte[] decodeSecretKey(String encryptKey) {
        String trimmed = encryptKey.trim();
        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to raw 32-byte key support.
        }

        byte[] raw = trimmed.getBytes(StandardCharsets.UTF_8);
        if (raw.length == 32) {
            return raw;
        }

        throw new IllegalArgumentException("encrypt key must be 32 bytes or standard base64 for 32 bytes");
    }
}
