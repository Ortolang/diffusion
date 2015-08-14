package fr.ortolang.diffusion.security.authentication;

import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

public class TOTPHelper {
    
    private static final Random rand = new Random();

    public static String generateSecret() {
        byte[] buffer = new byte[20];
        rand.nextBytes(buffer);
        Base32 codec = new Base32();
        byte[] secretKey = Arrays.copyOf(buffer, 20);
        byte[] encodedKey = codec.encode(secretKey);
        return new String(encodedKey);
    }
    
    public static int getCode(String secret) {
        return generateTOTP(secret, getCurrentInterval());
    }

    public static boolean checkCode(String secret, long code) {
        long currentInterval = getCurrentInterval();
        long hash1 = generateTOTP(secret, currentInterval);
        long hash2 = generateTOTP(secret, currentInterval-1);
        if (hash1 == code || hash2 == code) {
            return true;
        }
        return false;
    }
    
    private static int generateTOTP(String secret, long time) {
        Base32 codec = new Base32();
        byte[] decodedKey = codec.decode(secret);
        byte[] msg = ByteBuffer.allocate(8).putLong(time).array();
        byte[] hash = hmacSha(decodedKey, msg);
        int offset = hash[hash.length - 1] & 0xf;
        int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16) | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
        int otp = binary % 1000000;
        return otp;
    }

    private static byte[] hmacSha(byte[] keyBytes, byte[] text) {
        try {
            Mac hmac;
            hmac = Mac.getInstance("HmacSHA1");
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(text);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }

    private static long getCurrentInterval() {
        //30 minutes interval
        long interval = System.currentTimeMillis() / (1000 * 60 * 30);
        return interval;
    }

}
