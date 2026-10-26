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

    /** fid trong duong dan phai la cua ho dang dang nhap, khong cho phep tham so hoa tenant. */
    public Long requireFamily(Long familyIdInPath) {
        AuthPrincipal principal = require();
        if (familyIdInPath == null || !familyIdInPath.equals(principal.familyId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy hộ gia đình.");
        }
        return familyIdInPath;
    }

    /** OWNER / ADMIN moi duoc thay doi danh muc, vi, thanh vien (BR-02). */
    public AuthPrincipal requireManager() {
        AuthPrincipal principal = require();
        if (!principal.canManageFamily()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE",
                "Chỉ chủ hộ hoặc quản trị hộ mới thực hiện được thao tác này.");
        }
        return principal;
    }

    public AuthPrincipal requireWriter() {
        AuthPrincipal principal = require();
        if (!principal.canWrite()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE",
                "Quyền Viewer chỉ xem được dữ liệu.");
        }
        return principal;
    }
}
