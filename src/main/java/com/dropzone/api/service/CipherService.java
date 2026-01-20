package com.dropzone.api.service;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class CipherService {

    private static final String ALGORITHM = "AES";

    // 1. Generate a random AES Key
    // We will generate a unique key for EVERY file.
    public SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256); // AES-256 (Strong Security)
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES Algorithm not found", e);
        }
    }

    // Convert Key to String (to save in DB)
    public String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Convert String back to Key (to use for decryption)
    public SecretKey stringToKey(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    // 2. The Encryptor (Wraps the Output Stream)
    // Whatever we write to this stream gets encrypted before hitting the disk.
    public OutputStream encryptStream(OutputStream outputStream, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return new CipherOutputStream(outputStream, cipher);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing encryption", e);
        }
    }

    // 3. The Decryptor (Wraps the Input Stream)
    // Whatever we read from the disk gets decrypted before the user sees it.
    public InputStream decryptStream(InputStream inputStream, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new CipherInputStream(inputStream, cipher);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing decryption", e);
        }
    }
}