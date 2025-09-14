package com.anyang.maruni.global.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 암호화 서비스
 *
 * 보호자 연락처 등 민감한 정보를 암호화/복호화합니다.
 * GCM 모드를 사용하여 인증된 암호화를 제공합니다.
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private final SecretKey secretKey;

    public EncryptionService(@Value("${maruni.encryption.key}") String encryptionKey) {
        // 환경변수에서 받은 키를 SecretKey로 변환
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);

        // 키 길이를 32바이트(256비트)로 맞춤
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));

        this.secretKey = new SecretKeySpec(key, ALGORITHM);

        log.info("EncryptionService initialized with AES-256-GCM");
    }

    /**
     * 문자열을 암호화합니다.
     *
     * @param plainText 암호화할 평문
     * @return Base64 인코딩된 암호화된 텍스트 (IV + 암호화된 데이터 + 인증 태그)
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // 랜덤 IV 생성
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호화된 데이터를 합쳐서 반환
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);

        } catch (Exception e) {
            log.error("암호화 실패", e);
            throw new EncryptionException("암호화에 실패했습니다", e);
        }
    }

    /**
     * 암호화된 문자열을 복호화합니다.
     *
     * @param encryptedText Base64 인코딩된 암호화된 텍스트
     * @return 복호화된 평문
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

            // IV 추출
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);

            // 암호화된 데이터 추출
            byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decryptedData = cipher.doFinal(cipherText);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("복호화 실패", e);
            throw new EncryptionException("복호화에 실패했습니다", e);
        }
    }

    /**
     * 암호화 가능 여부를 확인합니다.
     *
     * @return 암호화 서비스 사용 가능 여부
     */
    public boolean isAvailable() {
        try {
            String testText = "test";
            String encrypted = encrypt(testText);
            String decrypted = decrypt(encrypted);
            return testText.equals(decrypted);
        } catch (Exception e) {
            log.error("암호화 서비스 사용 불가", e);
            return false;
        }
    }
}