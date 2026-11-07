package com.antest1.gotobrowser.Helpers;

import static com.antest1.gotobrowser.Constants.PREF_DMM_PASS;

import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Keeps the login autocomplete password encrypted in the shared preferences,
 * with an AES-GCM key held by the Android Keystore. The key never leaves the
 * device, so a password restored from a backup onto another device can't be
 * read and is dropped instead.
 */
public class CredentialStore {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "gotobrowser_login_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String PREFIX = "enc1:";
    private static final int TAG_BITS = 128;

    /**
     * Encrypts a password saved in plain text by an older version. Called at app
     * start so the plain text does not stay on disk until the next login. If the
     * encryption fails, the password is left as it is and retried next time.
     */
    public static void migrateLegacyPassword(SharedPreferences pref) {
        String stored = pref.getString(PREF_DMM_PASS, "");
        if (stored != null && !stored.isEmpty() && !stored.startsWith(PREFIX)) {
            setPassword(pref, stored);
        }
    }

    /** Returns the saved password, or "" when none is saved or it can't be decrypted. */
    public static String getPassword(SharedPreferences pref) {
        String stored = pref.getString(PREF_DMM_PASS, "");
        if (stored == null || stored.isEmpty()) return "";

        if (!stored.startsWith(PREFIX)) {
            // Saved in plain text by an older version: encrypt it in place.
            setPassword(pref, stored);
            return stored;
        }

        try {
            return decrypt(stored.substring(PREFIX.length()));
        } catch (Exception e) {
            // The key is gone (app data restored on another device, or the
            // keystore was reset): the value can never be read again.
            KcUtils.reportException(e);
            pref.edit().remove(PREF_DMM_PASS).apply();
            return "";
        }
    }

    /**
     * Saves the password encrypted, or removes it when empty.
     * Returns false if it could not be encrypted; nothing is saved then.
     */
    public static boolean setPassword(SharedPreferences pref, String password) {
        if (password == null || password.isEmpty()) {
            pref.edit().remove(PREF_DMM_PASS).apply();
            return true;
        }
        try {
            pref.edit().putString(PREF_DMM_PASS, PREFIX + encrypt(password)).apply();
            return true;
        } catch (Exception e) {
            KcUtils.reportException(e);
            return false;
        }
    }

    private static String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
                + ":" + Base64.encodeToString(cipherText, Base64.NO_WRAP);
    }

    private static String decrypt(String encoded) throws Exception {
        String[] parts = encoded.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("malformed password data");
        SecretKey key = getKey();
        if (key == null) throw new IllegalStateException("login key not found");

        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] cipherText = Base64.decode(parts[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    private static SecretKey getKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }

    // Synchronized so that two callers never both generate a key: the second one
    // would replace the first, and a password encrypted with it could not be read.
    private static synchronized SecretKey getOrCreateKey() throws Exception {
        SecretKey key = getKey();
        if (key != null) return key;

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
