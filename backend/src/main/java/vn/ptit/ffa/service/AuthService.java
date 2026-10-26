package vn.ptit.ffa.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.ffa.domain.Family;
import vn.ptit.ffa.domain.FamilyMember;
import vn.ptit.ffa.domain.MemberRole;
import vn.ptit.ffa.domain.User;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.repository.FamilyMemberRepository;
import vn.ptit.ffa.repository.FamilyRepository;
import vn.ptit.ffa.repository.UserRepository;
import vn.ptit.ffa.security.AuthPrincipal;
import vn.ptit.ffa.security.JwtService;
import vn.ptit.ffa.security.RefreshTokenService;
import vn.ptit.ffa.web.dto.AuthDto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Nghiêp vụ xác thực: đăng ký, đăng nhập, refresh, logout (DUAN-39). */
@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository users;
    private final FamilyRepository families;
    private final FamilyMemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final Cache<String, Integer> failedLogins = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .maximumSize(10_000)
        .build();

    public AuthService(UserRepository users, FamilyRepository families, FamilyMemberRepository members,
                       PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokens) {
        this.users = users;
        this.families = families;
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email này đã được đăng ký.");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setActive(true);
        user.setAdmin(false);
        users.save(user);

        String familyName = (request.familyName() == null || request.familyName().isBlank())
            ? "Hộ của " + request.fullName()
            : request.familyName().trim();
        if (families.existsByName(familyName)) {
            familyName = familyName + " (" + user.getId() + ")";
        }
        Family family = new Family();
        family.setName(familyName);
        family.setBaseCurrency("VND");
        family.setFiscalMonthStartDay(1);
        family.setCreatedBy(user.getId());
        families.save(family);

        FamilyMember membership = new FamilyMember();
        membership.setFamilyId(family.getId());
        membership.setUserId(user.getId());
        membership.setRole(MemberRole.OWNER);
        members.save(membership);

        return issue(user, family);
    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        Integer failures = failedLogins.getIfPresent(request.email());
        if (failures != null && failures >= MAX_FAILED_ATTEMPTS) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                "Tài khoản tạm khoá vì đăng nhập sai nhiều lần. Thử lại sau 15 phút.");
        }
        User user = users.findByEmail(request.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            failedLogins.put(request.email(), (failures == null ? 0 : failures) + 1);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS",
                "Email hoặc mật khẩu không đúng.");
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE", "Tài khoản đã bị khoá.");
        }
        failedLogins.invalidate(request.email());
        user.setLastLoginAt(LocalDateTime.now());
        users.save(user);

        FamilyMember membership = members.findByUserId(user.getId()).stream().findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE",
                "Tài khoản chưa thuộc hộ gia đình nào."));
        Family family = families.findById(membership.getFamilyId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy hộ gia đình."));
        return issue(user, family);
    }

    public AuthDto.AuthResponse refresh(AuthDto.RefreshRequest request) {
        var issued = refreshTokens.rotate(request.refreshToken());
        User user = users.findById(issued.entity().getUserId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REVOKED", "Phiên không còn hiệu lực."));
        FamilyMember membership = members.findByUserId(user.getId()).stream().findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE", "Tài khoản chưa có hộ gia đình."));
        Family family = families.findById(membership.getFamilyId()).orElseThrow();
        return build(user, family, membership.getRole(), issued.raw());
    }

    public void logout(AuthDto.LogoutRequest request) {
        refreshTokens.revoke(request.refreshToken());
    }

    public AuthDto.MeResponse me(AuthPrincipal principal) {
        User user = users.findById(principal.userId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy người dùng."));
        Family active = families.findById(principal.familyId()).orElseThrow();
        return new AuthDto.MeResponse(userView(user), familyView(active, principal.role()), familyViews(user.getId()));
    }

    private AuthDto.AuthResponse issue(User user, Family family) {
        MemberRole role = members.findByFamilyIdAndUserId(family.getId(), user.getId())
            .map(FamilyMember::getRole)
            .orElse(MemberRole.MEMBER);
        var issued = refreshTokens.issue(user.getId());
        return build(user, family, role, issued.raw());
    }

    private AuthDto.AuthResponse build(User user, Family family, MemberRole role, String refreshRaw) {
        AuthPrincipal principal = new AuthPrincipal(user.getId(), family.getId(), role, user.getEmail(), user.isAdmin());
        return new AuthDto.AuthResponse(
            jwtService.issueAccessToken(principal),
            refreshRaw,
            "Bearer",
            jwtService.accessTtl().getSeconds(),
            userView(user),
            familyViews(user.getId()));
    }

    private List<AuthDto.FamilyView> familyViews(Long userId) {
        return members.findByUserId(userId).stream()
            .map(membership -> families.findById(membership.getFamilyId())
                .map(family -> familyView(family, membership.getRole()))
                .orElse(null))
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    private AuthDto.UserView userView(User user) {
        return new AuthDto.UserView(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), user.isAdmin());
    }

    private AuthDto.FamilyView familyView(Family family, MemberRole role) {
        return new AuthDto.FamilyView(family.getId(), family.getName(), family.getBaseCurrency(), role.name());
    }
}
