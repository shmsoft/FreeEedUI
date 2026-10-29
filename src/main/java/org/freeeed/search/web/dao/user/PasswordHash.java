/*
 *
 * Copyright SHMsoft, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.freeeed.search.web.dao.user;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 
 * Class PasswordHash.
 * 
 * Password hashing for stored review-app credentials. Passwords used to be held
 * and compared as plaintext, so anyone who could read work/u.dat had every
 * reviewer's password (issue #68).
 * 
 * PBKDF2-HMAC-SHA256 from the JDK, deliberately: the review app ships with
 * outstanding dependency advisories, so this adds no new jar to the war. The
 * stored form is self-describing, which lets the work factor be raised later
 * without invalidating existing hashes:
 * 
 *   pbkdf2$&lt;iterations&gt;$&lt;base64 salt&gt;$&lt;base64 hash&gt;
 * 
 * {@link #verify(String, String)} also accepts a legacy plaintext value so that
 * an existing work/u.dat keeps working; the DAO upgrades such a record to a
 * hash on the next successful login.
 * 
 */
public final class PasswordHash {
    private static final String PREFIX = "pbkdf2$";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    /** Unambiguous alphabet -- no O/0/I/l/1, because this gets read off a screen and retyped. */
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GENERATED_LENGTH = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHash() {
    }

    /**
     * Hash a plaintext password for storage. Returns the self-describing form
     * described above; never returns the plaintext.
     */
    public static String hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("password must not be null");
        }

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] key = derive(plaintext, salt, ITERATIONS);
        Base64.Encoder enc = Base64.getEncoder();

        return PREFIX + ITERATIONS + "$" + enc.encodeToString(salt) + "$" + enc.encodeToString(key);
    }

    /**
     * Check a candidate password against a stored value. Accepts both the
     * hashed form and a legacy plaintext value, so upgrading an install does
     * not lock existing users out.
     */
    public static boolean verify(String candidate, String stored) {
        if (candidate == null || stored == null) {
            return false;
        }

        if (!isHashed(stored)) {
            // Legacy plaintext record. Constant-time compare so this path does
            // not leak the password's length or prefix through timing either.
            return MessageDigest.isEqual(candidate.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder dec = Base64.getDecoder();
            byte[] salt = dec.decode(parts[2]);
            byte[] expected = dec.decode(parts[3]);

            return MessageDigest.isEqual(derive(candidate, salt, iterations), expected);
        } catch (RuntimeException e) {
            // A malformed stored value must fail closed, not throw into the
            // login controller.
            return false;
        }
    }

    /** True when the stored value is already hashed (as opposed to legacy plaintext). */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    /**
     * A random password for the first-run admin account, from an alphabet with
     * no visually ambiguous characters.
     */
    public static String generatePassword() {
        StringBuilder sb = new StringBuilder(GENERATED_LENGTH);
        for (int i = 0; i < GENERATED_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static byte[] derive(String plaintext, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(plaintext.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash password with " + ALGORITHM, e);
        } finally {
            spec.clearPassword();
        }
    }
}
