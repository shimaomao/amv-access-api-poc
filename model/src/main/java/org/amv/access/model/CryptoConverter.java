package org.amv.access.model;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.security.Key;
import java.util.Base64;

/**
 * An encrypted column converter for JPA entities.
 * <p>
 * You must use {@link #initKey(byte[])} before using this class.
 * <p>
 * Usage in JPA entity:
 * <pre><code>
 * &#064;Entity
 * &#064;Table(name = "my_entity")
 * public class MyEntity {
 *   &#064;Column(name = "value")
 *   &#064;Convert(converter = CryptoConverter.class)
 *   private String value;
 * }
 * </code></pre>
 */
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {
    static {
        String defaultKey = "MySuperSecretKey";
        initKey(defaultKey.getBytes());
    }

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static Key key;

    public static void initKey(byte[] key) {
        CryptoConverter.key = new SecretKeySpec(key, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String val) {
        if (val == null) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(val.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbVal) {
        if (dbVal == null) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbVal)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}