package vn.ptit.ffa.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.ffa.domain.RefreshToken;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.repository.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh token là chuỗi ngẫu nhiên opaque (không phải JWT), chỉ lưu SHA-256 hash
 * trong DB. Mỗi lần refresh thu hồi token cũ và phát token mới (DUAN-39).
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repository,
                              @Value("${app.jwt.refresh-token-days}") long days) {
        this.repository = repository;
        this.ttl = Duration.ofDays(days);
    }

    public record Issued(String raw, RefreshToken entity) {
    }

    @Transactional
    public Issued issue(Long userId) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(sha256(raw));
        entity.setExpiresAt(LocalDateTime.now().plus(ttl));
        return new Issued(raw, repository.save(entity));
    }

    @Transactional
    public Issued rotate(String raw) {
        RefreshToken current = findValid(raw);
        current.setRevokedAt(LocalDateTime.now());
        repository.save(current);
        return issue(current.getUserId());
    }

    @Transactional
    public void revoke(String raw) {
        findValidOptional(raw).ifPresent(token -> {
            token.setRevokedAt(LocalDateTime.now());
            repository.save(token);
        });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllByUserId(userId, LocalDateTime.now());
    }

    private RefreshToken findValid(String raw) {
        return findValidOptional(raw).orElseThrow(() ->
            new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "REFRESH_TOKEN_REVOKED", "Refresh token không còn hiệu lực, phải đăng nhập lại."));
    }

    private java.util.Optional<RefreshToken> findValidOptional(String raw) {
        return repository.findByTokenHash(sha256(raw))
            .filter(token -> token.getRevokedAt() == null)
            .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 khong ho tro tren JVM nay", e);
        }
    }
}
