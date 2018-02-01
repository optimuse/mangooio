package io.mangoo.utils;

import java.security.SecureRandom;
import java.util.Objects;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

import io.mangoo.enums.Required;

/**
 *
 * @author svenkubiak
 *
 */
public final class CryptoUtils {
    private static final char[] CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final int MAX_PASSWORD_LENGTH = 256;
    private static final int MIN_PASSWORD_LENGTH = 0;
    private static final int MAX_KEY_LENGTH = 32;
    private static final int KEYINDEX_START = 0;
    
    private CryptoUtils() {
    }
    
    /**
     * Creates a secret for encrypt or decryption which has a length
     * of 32 characters, corresponding to 256 Bits
     * 
     * If the provided secret has more than 32 characters it will be trimmed
     * to 32 characters.
     *
     * @param secret A given secret to trim
     * @return A secret with at least 32 characters
     */
    public static String getSizedSecret(String secret) {
        Objects.requireNonNull(secret, Required.SECRET.toString());
        String key = StringUtils.replaceAll(secret, "[^\\x00-\\x7F]", "");
        
        return key.length() < MAX_KEY_LENGTH ? key : key.substring(KEYINDEX_START, MAX_KEY_LENGTH);
    }
    
    /**
     * Checks if the given secret has at least 32 characters
     * 
     * @param secret The secret to check
     * @return True if secret is at least 32 characters, false if not
     */
    public static boolean isValidSecret(String secret) {
        Objects.requireNonNull(secret, Required.SECRET.toString());
        String key = StringUtils.replaceAll(secret, "[^\\x00-\\x7F]", "");

        return key.length() >= MAX_KEY_LENGTH;
    }
    
    /**
     * Generates a random string with the given length.
     * 
     * Based on commons-lang3 RandomStringUtils using SecureRandom
     * 
     * Uses: uppercase letters, lowercase letters and numbers
     * 
     * @param length The length of the random string
     * @return A random String
     */
    public static String randomString(int length) {
        Preconditions.checkArgument(length > MIN_PASSWORD_LENGTH, "password length must be at least 1 character");
        Preconditions.checkArgument(length <= MAX_PASSWORD_LENGTH, "password length must be at most 256 character");
        
        return RandomStringUtils.random(length, 0, CHARACTERS.length-1, false, false, CHARACTERS, new SecureRandom());
    }
}
