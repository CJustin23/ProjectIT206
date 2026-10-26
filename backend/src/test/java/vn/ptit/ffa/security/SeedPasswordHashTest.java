package vn.ptit.ffa.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hash demo trong V4__fix_demo_password_hash.sql phai khop mat khau ghi o README
 * va api-smoke — sai thi CI do ngay o buoc dang nhap.
 */
class SeedPasswordHashTest {

    private static final String DEMO_HASH =
        "$2a$10$qtbi313Z4lrlBqdfKHJ7nekX0nJcEyxfoyzQqmAd01NDOJnoHkJQO";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Test
    void demoHashMatchesPassword() {
        assertTrue(encoder.matches("password", DEMO_HASH),
            "Hash trong V4__fix_demo_password_hash.sql phải khớp mật khẩu demo 'password'");
    }

    @Test
    void rejectsWrongPassword() {
        assertFalse(encoder.matches("mat-khau-sai", DEMO_HASH));
    }

    @Test
    void generatedHashVerifies() {
        String hash = encoder.encode("password");
        assertTrue(encoder.matches("password", hash));
    }
}
