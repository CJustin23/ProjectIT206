package vn.ptit.ffa.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** DTO ve ho gia dinh va thanh vien (api.md muc 2.2). */
public final class FamilyDto {

    private FamilyDto() {
    }

    public record CreateRequest(@NotBlank String name) {
    }

    public record InviteRequest(@NotBlank @Email String email, @NotBlank String role) {
    }

    public record RoleRequest(@NotBlank String role) {
    }

    public record MemberView(Long id,
                             Long userId,
                             String email,
                             String fullName,
                             String role,
                             LocalDateTime joinedAt,
                             boolean active) {
    }

    public record FamilyView(Long id, String name, String baseCurrency, String myRole, int memberCount) {
    }
}
