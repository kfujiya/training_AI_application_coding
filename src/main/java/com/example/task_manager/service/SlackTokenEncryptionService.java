package com.example.task_manager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SlackTokenEncryptionService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final String encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public SlackTokenEncryptionService(@Value("${slack.oauth.encryption-key:}") String encryptionKey) {
        this.encryptionKey = encryptionKey == null ? "" : encryptionKey;
    }

    public String encrypt(String token) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (Exception e) {
            throw new SlackTokenEncryptionException();
        }
    }

    public String decrypt(String encryptedToken) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedToken);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SlackTokenEncryptionException();
        }
    }

    private SecretKeySpec key() {
        if (encryptionKey.isBlank()) throw new SlackTokenEncryptionException();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new SlackTokenEncryptionException();
        }
    }

    public static class SlackTokenEncryptionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
