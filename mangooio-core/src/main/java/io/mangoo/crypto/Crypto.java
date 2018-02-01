package io.mangoo.crypto;

import java.util.Base64;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

import io.mangoo.configuration.Config;
import io.mangoo.enums.Required;
import io.mangoo.utils.CryptoUtils;

/**
 * Convenient class for encryption and decryption
 *
 * @author svenkubiak
 *
 */
public class Crypto {
    private static final Logger LOG = LogManager.getLogger(Crypto.class);
    private static final Base64.Encoder base64Encoder = Base64.getEncoder();
    private static final Base64.Decoder base64Decoder = Base64.getDecoder();
    private final PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESLightEngine()));
    private Config config;
    
    @Inject
    public Crypto(Config config) {
        this.config = Objects.requireNonNull(config, Required.CONFIG.toString());
    }
    
    /**
     * Decrypts an given encrypted text using the application secret property (application.secret) as key
     *
     * @param encrytedText The encrypted text
     * @return The clear text or null if decryption fails
     */
    public String decrypt(String encrytedText) {
        Objects.requireNonNull(encrytedText, Required.ENCRYPTED_TEXT.toString());

        return decrypt(encrytedText, CryptoUtils.getSizedSecret(this.config.getApplicationSecret()));
    }

    /**
     * Decrypts an given encrypted text using the given key
     *
     * @param encrytedText The encrypted text
     * @param key The encryption key
     * @return The clear text or null if decryption fails
     */
    public String decrypt(String encrytedText, String key) {
        Objects.requireNonNull(encrytedText, Required.ENCRYPTED_TEXT.toString());
        Objects.requireNonNull(key, Required.KEY.toString());

        CipherParameters cipherParameters = new ParametersWithRandom(new KeyParameter(CryptoUtils.getSizedSecret(key).getBytes(Charsets.UTF_8)));
        this.cipher.init(false, cipherParameters);
        
        return new String(cipherData(base64Decoder.decode(encrytedText)), Charsets.UTF_8);
    }

    /**
     * Encrypts a given plain text using the application secret property (application.secret) as key
     *
     * Encryption is done by using AES and CBC Cipher and a key length of 256 bit
     *
     * @param plainText The plain text to encrypt
     * @return The encrypted text or null if encryption fails
     */
    public String encrypt(String plainText) {
        Objects.requireNonNull(plainText, Required.PLAIN_TEXT.toString());

        return encrypt(plainText, CryptoUtils.getSizedSecret(this.config.getApplicationSecret()));
    }

    /**
     * Encrypts a given plain text using the given key
     *
     * Encryption is done by using AES and CBC Cipher and a key length of 256 bit
     *
     * @param plainText The plain text to encrypt
     * @param key The key to use for encryption
     * @return The encrypted text or null if encryption fails
     */
    public String encrypt(String plainText, String key) {
        Objects.requireNonNull(plainText, Required.PLAIN_TEXT.toString());
        Objects.requireNonNull(key, Required.KEY.toString());

        CipherParameters cipherParameters = new ParametersWithRandom(new KeyParameter(CryptoUtils.getSizedSecret(key).getBytes(Charsets.UTF_8)));
        this.cipher.init(true, cipherParameters);

        return new String(base64Encoder.encode(cipherData(plainText.getBytes(Charsets.UTF_8))), Charsets.UTF_8);
    }

    /**
     * Encrypts or decrypts a given byte array of data
     *
     * @param data The data to encrypt or decrypt
     * @return A clear text or encrypted byte array
     */
    private byte[] cipherData(byte[] data) {
        byte[] result = null;
        try {
            final byte[] buffer = new byte[this.cipher.getOutputSize(data.length)];

            final int processedBytes = this.cipher.processBytes(data, 0, data.length, buffer, 0);
            final int finalBytes = this.cipher.doFinal(buffer, processedBytes);

            result = new byte[processedBytes + finalBytes];
            System.arraycopy(buffer, 0, result, 0, result.length);
        } catch (final CryptoException e) {
            LOG.error("Failed to encrypt/decrypt", e);
        }

        return result;
    }
}