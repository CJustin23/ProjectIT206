package vn.ptit.ffa.service;

import org.springframework.http.HttpStatus;
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
import vn.ptit.ffa.web.dto.FamilyDto;

import java.util.List;

/** Quản lý hộ gia đình: danh sách, mời, đổi vai trò, gỡ thành viên (DUAN-40). */
@Service
public class FamilyService {

    private final FamilyRepository families;
    private final FamilyMemberRepository members;
    private final UserRepository users;

    public FamilyService(FamilyRepository families, FamilyMemberRepository members, UserRepository users) {
        this.families = families;
        this.members = members;
        this.users = users;
    }

    @Transactional
    public FamilyDto.FamilyView create(AuthPrincipal principal, String name) {
        String familyName = name.trim();
        if (families.existsByName(familyName)) {
            throw new ApiException(HttpStatus.CONFLICT, "FAMILY_NAME_EXISTS", "Tên hộ gia đình đã tồn tại.");
        }
        Family family = new Family();
        family.setName(familyName);
        family.setBaseCurrency("VND");
        family.setFiscalMonthStartDay(1);
        family.setCreatedBy(principal.userId());
        families.save(family);

        FamilyMember membership = new FamilyMember();
        membership.setFamilyId(family.getId());
        membership.setUserId(principal.userId());
        membership.setRole(MemberRole.OWNER);
        members.save(membership);

        return new FamilyDto.FamilyView(family.getId(), family.getName(), family.getBaseCurrency(),
            MemberRole.OWNER.name(), 1);
    }

    @Transactional(readOnly = true)
    public List<FamilyDto.MemberView> members(AuthPrincipal principal, Long familyId) {
        requireFamily(principal, familyId);
        return members.findByFamilyId(familyId).stream()
            .map(membership -> users.findById(membership.getUserId()).map(user -> new FamilyDto.MemberView(
                membership.getId(), user.getId(), user.getEmail(), user.getFullName(),
                membership.getRole().name(), membership.getJoinedAt(), user.isActive()))
                .orElse(null))
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    @Transactional
    public void invite(AuthPrincipal principal, Long familyId, FamilyDto.InviteRequest request) {
        requireManager(principal);
        requireFamily(principal, familyId);
        User user = users.findByEmail(request.email().trim())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "Email này chưa đăng ký tài khoản. Người được mời phải đăng ký trước."));
        if (members.existsByFamilyIdAndUserId(familyId, user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_MEMBER", "Người này đã là thành viên của hộ.");
        }
        FamilyMember membership = new FamilyMember();
        membership.setFamilyId(familyId);
        membership.setUserId(user.getId());
        membership.setRole(parseRole(request.role()));
        members.save(membership);
    }

    @Transactional
    public void changeRole(AuthPrincipal principal, Long familyId, Long userId, String role) {
        requireManager(principal);
        requireFamily(principal, familyId);
        FamilyMember target = members.findByFamilyIdAndUserId(familyId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không phải thành viên của hộ."));
        if (target.getRole() == MemberRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OWNER_IMMUTABLE",
                "Không đổi được vai trò của chủ hộ, hãy dùng chức năng chuyển giao.");
        }
        target.setRole(parseRole(role));
        members.save(target);
    }

    @Transactional
    public void remove(AuthPrincipal principal, Long familyId, Long userId) {
        requireManager(principal);
        requireFamily(principal, familyId);
        FamilyMember target = members.findByFamilyIdAndUserId(familyId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không phải thành viên của hộ."));
        if (target.getRole() == MemberRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OWNER_IMMUTABLE", "Không gỡ được chủ hộ.");
        }
        members.delete(target);
    }

    private void requireFamily(AuthPrincipal principal, Long familyId) {
        if (familyId == null || !familyId.equals(principal.familyId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy hộ gia đình.");
        }
    }

    private void requireManager(AuthPrincipal principal) {
        if (!principal.canManageFamily()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE",
                "Chỉ chủ hộ hoặc quản trị hộ mới thêm được thành viên.");
        }
    }

    private MemberRole parseRole(String value) {
        try {
            var parsed = MemberRole.valueOf(value.trim().toUpperCase());
            if (parsed == MemberRole.OWNER) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Không gán trực tiếp vai trò OWNER.");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "role chỉ nhận ADMIN, MEMBER hoặc VIEWER.");
        }
    }
}
