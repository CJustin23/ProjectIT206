package vn.ptit.ffa.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.ptit.ffa.domain.MemberRole;
import vn.ptit.ffa.error.ApiException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Ký và xác minh access token HS256 (DUAN-39). */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-minutes}") long accessMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "app.jwt.secret phai dai it nhat 32 byte — sinh bang: openssl rand -base64 48");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(accessMinutes);
    }

    public String issueAccessToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(principal.userId()))
            .claim("email", principal.email())
            .claim("fam", principal.familyId())
            .claim("rol", principal.role().name())
            .claim("adm", principal.systemAdmin())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessTtl)))
            .signWith(key)
            .compact();
    }

    public AuthPrincipal verify(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new AuthPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("fam", Number.class).longValue(),
                MemberRole.valueOf(claims.get("rol", String.class)),
                claims.get("email", String.class),
                Boolean.TRUE.equals(claims.get("adm", Boolean.class)));
        } catch (ExpiredJwtException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Access token đã hết hạn.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Token không hợp lệ.");
        }
    }

    public Duration accessTtl() {
        return accessTtl;
    }
}
