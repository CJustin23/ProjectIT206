package vn.ptit.ffa.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.ptit.ffa.error.ApiException;

/** Lay danh tinh dang xu ly. Khong cho controller tu doc SecurityContext. */
@Component
public class CurrentUser {

    public AuthPrincipal require() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Chưa đăng nhập.");
        }
        return principal;
    }

    public Long familyId() {
        return require().familyId();
    }

    public Long userId() {
        return require().userId();
    }
}
