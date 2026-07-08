package com.arjunsports.contentagent.common.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialEncryptionUtilTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final CredentialEncryptionUtil util = new CredentialEncryptionUtil(KEY);

    @Test
    void encryptThenDecrypt_returnsOriginalPlaintext() {
        String plaintext = "IGQVJYbG9zZWNyZXR0b2tlbnZhbHVl";

        String encrypted = util.encrypt(plaintext);
        String decrypted = util.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_neverReturnsThePlaintext() {
        String plaintext = "super-secret-access-token";

        String encrypted = util.encrypt(plaintext);

        assertThat(encrypted).doesNotContain(plaintext);
    }

    @Test
    void encrypt_sameInputTwice_producesDifferentCiphertext() {
        String plaintext = "same-token-both-times";

        String first = util.encrypt(plaintext);
        String second = util.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
        assertThat(util.decrypt(first)).isEqualTo(plaintext);
        assertThat(util.decrypt(second)).isEqualTo(plaintext);
    }

    @Test
    void decrypt_tamperedCiphertext_throws() {
        String encrypted = util.encrypt("a-token-value");
        byte[] bytes = Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> util.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_blankKey_throws() {
        assertThatThrownBy(() -> new CredentialEncryptionUtil(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_wrongLengthKey_throws() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new CredentialEncryptionUtil(shortKey))
                .isInstanceOf(IllegalStateException.class);
    }
}
