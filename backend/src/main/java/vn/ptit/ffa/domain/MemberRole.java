package vn.ptit.ffa.domain;

/** Vai trò của một user trong hộ gia đình (BR-02). */
public enum MemberRole {
    OWNER, ADMIN, MEMBER, VIEWER;

    public boolean canEdit() {
        return this != VIEWER;
    }

    public boolean isManager() {
        return this == OWNER || this == ADMIN;
    }
}
