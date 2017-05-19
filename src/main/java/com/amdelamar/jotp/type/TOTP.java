package com.amdelamar.jotp.type;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.amdelamar.jotp.exception.BadOperationException;

/**
 * Time based OTP class implements OTPInterface
 * 
 * @author kamranzafar, amdelamar
 * @see https://tools.ietf.org/html/rfc6238
 */
public class TOTP implements OTPInterface {
    
    /**
     * HmacSHA1, HmacSHA256, HmacSHA512
     */
    private static final String HMACSHA1_ALGORITHM = "HmacSHA1";

    /**
     * Create a one-time-password with the given key, base, and digits.
     * 
     * @param secret
     *            The secret. Shhhhhh!
     * @param base
     *            The offset. (TOTP base is time from UTC rounded to the half-second)
     * @param digits
     *            The length of the code (Commonly '6')
     * @return code
     * @throws BadOperationException
     * @see https://tools.ietf.org/html/rfc6238
     */
    public String create(String secret, String base, int digits) {
        try {
            return generateTotp(secret, base, digits, HMACSHA1_ALGORITHM);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method uses the JCE to provide the crypto algorithm. HMAC computes a Hashed Message
     * Authentication Code with the crypto hash algorithm as a parameter.
     * 
     * @param crypto
     *            the crypto algorithm (HmacSHA1, HmacSHA256, HmacSHA512)
     * @param keyBytes
     *            the bytes to use for the HMAC key
     * @param text
     *            the message or text to be authenticated
     */
    private static byte[] hmac(String crypto, byte[] keyBytes, byte[] text) {
        try {
            Mac hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(text);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }

    /**
     * This method converts a HEX string to Byte[]
     * 
     * @param hex
     *            the HEX string
     * @returna byte array
     */
    private static byte[] hexStr2Bytes(String hex) {
        // Adding one byte to get the right conversion
        // Values starting with "0" can be converted
        byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

        // Copy all the REAL bytes, not the "first"
        byte[] ret = new byte[bArray.length - 1];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = bArray[i + 1];
        }
        return ret;
    }

    /**
     * This method generates a TOTP value for the given set of parameters.
     * 
     * @param key
     *            the shared secret, HEX encoded
     * @param time
     *            a value that reflects a time
     * @param returnDigits
     *            number of digits to return
     * @param crypto
     *            the crypto function to use
     * @returna numeric String in base 10 that includes {@link truncationDigits} digits
     */
    private static String generateTotp(String key, String time, int digits, String crypto) {
        // Using the counter
        // First 8 bytes are for the movingFactor
        // Compliant with base RFC 4226 (HOTP)
        while (time.length() < 16) {
            time = "0" + time;
        }

        // Get the HEX in a Byte[]
        byte[] msg = hexStr2Bytes(time);
        byte[] k = hexStr2Bytes(key);

        byte[] hash = hmac(crypto, k, msg);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);

        int otp = binary % ((int) Math.pow(10, digits));

        String result = Integer.toString(otp);
        while (result.length() < digits) {
            result = "0" + result;
        }
        return result;
    }
}