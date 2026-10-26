package vn.ptit.ffa.security;

import vn.ptit.ffa.domain.MemberRole;

/**
 * Danh tính của request đang xử lý, lấy từ access token. familyId luôn là hộ đang
 * chọn của user — mọi truy vấn nghiệp vụ phải lọc theo giá trị này (BR-01).
 */
public record AuthPrincipal(Long userId, Long familyId, MemberRole role, String email, boolean systemAdmin) {

    public boolean canManageFamily() {
        return role != null && role.isManager();
    }

    public boolean canWrite() {
        return role != null && role.canEdit();
    }
}
