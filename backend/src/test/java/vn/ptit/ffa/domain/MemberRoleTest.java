package vn.ptit.ffa.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BR-02: vai trò quyết định quyền sửa dữ liệu của hộ. */
class MemberRoleTest {

    @Test
    void viewerReadOnly() {
        assertFalse(MemberRole.VIEWER.canEdit());
        assertFalse(MemberRole.VIEWER.isManager());
    }

    @Test
    void memberCanEditButNotManage() {
        assertTrue(MemberRole.MEMBER.canEdit());
        assertFalse(MemberRole.MEMBER.isManager());
    }

    @Test
    void adminAndOwnerManageMembers() {
        assertTrue(MemberRole.ADMIN.isManager());
        assertTrue(MemberRole.OWNER.isManager());
    }
}
