package at.fhtw.mrp.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

public final class PasswordUtil {

    private PasswordUtil() {}

    private static final int SALT_LEN = 16;
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LEN = 256;

    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LEN);
        // store: iterations:salt:hash (all Base64)
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(char[] password, String stored) {
        String[] parts = stored.split(":");
        int iters = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);
        byte[] actual = pbkdf2(password, salt, iters, expected.length * 8);
        return slowEquals(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLenBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLenBits);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

}
