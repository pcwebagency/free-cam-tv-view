package it.pcwebagency.telecamere;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Cifratura delle password con una chiave AES-256 dell'Android Keystore (non estraibile dal
 * dispositivo). Su Android 5.1, privo del Keystore AES, le password restano in chiaro.
 */
public final class Crypto {
    private static final String ALIAS = "credenziali-telecamere";
    private static final String PREFIX = "enc1:";
    private static final int IV_BYTES = 12;

    private Crypto() {}

    public static boolean isEncrypted(String stored) {
        return stored.startsWith(PREFIX);
    }

    public static boolean isAvailable() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public static String encrypt(String plain) {
        if (plain.isEmpty() || !isAvailable()) return plain;
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
            return PREFIX + Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e("FreeCamTVView", "Cifratura non riuscita, password salvata in chiaro", e);
            return plain;
        }
    }

    /** Restituisce la password in chiaro; "" se la chiave non esiste più (app reinstallata). */
    public static String decrypt(String stored) {
        if (!isEncrypted(stored)) return stored;
        if (!isAvailable()) return "";
        try {
            byte[] data = Base64.decode(stored.substring(PREFIX.length()), Base64.NO_WRAP);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, data, 0, IV_BYTES));
            return new String(c.doFinal(data, IV_BYTES, data.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e("FreeCamTVView", "Decifratura non riuscita: password da reinserire", e);
            return "";
        }
    }

    @RequiresApi(23)
    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey) ks.getKey(ALIAS, null);
        KeyGenerator g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        g.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return g.generateKey();
    }
}
