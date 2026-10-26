package vn.ptit.ffa.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** DTO cua phan he xac thuc (DUAN-39, DUAN-40). */
public final class AuthDto {

    private AuthDto() {
    }

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 120) String familyName) {
    }

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken,
                               String refreshToken,
                               String tokenType,
                               long expiresInSeconds,
                               UserView user,
                               List<FamilyView> families) {
    }

    public record UserView(Long id, String email, String fullName, String phone, boolean systemAdmin) {
    }

    public record FamilyView(Long id, String name, String baseCurrency, String role) {
    }

    public record MemberView(Long userId, String email, String fullName, String role, Object joinedAt) {
    }

    public record MeResponse(UserView user, FamilyView family, List<FamilyView> families) {
    }
}
