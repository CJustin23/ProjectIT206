package vn.ptit.ffa.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ptit.ffa.repository.FamilyMemberRepository;
import vn.ptit.ffa.repository.FamilyRepository;
import vn.ptit.ffa.security.CurrentUser;
import vn.ptit.ffa.service.FamilyService;
import vn.ptit.ffa.web.dto.FamilyDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    private final FamilyService families;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository memberRepository;
    private final CurrentUser currentUser;

    public FamilyController(FamilyService families, FamilyRepository familyRepository,
                            FamilyMemberRepository memberRepository, CurrentUser currentUser) {
        this.families = families;
        this.familyRepository = familyRepository;
        this.memberRepository = memberRepository;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<FamilyDto.FamilyView> create(@Valid @RequestBody FamilyDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(families.create(currentUser.require(), request.name()));
    }

    @GetMapping("/{id}")
    public FamilyDto.FamilyView get(@PathVariable Long id) {
        var principal = currentUser.requireFamily(id);
        var family = familyRepository.findById(principal)
            .orElseThrow(() -> new vn.ptit.ffa.error.ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy hộ gia đình."));
        var role = memberRepository.findByFamilyIdAndUserId(principal, currentUser.require().userId())
            .map(member -> member.getRole().name()).orElse(null);
        return new FamilyDto.FamilyView(family.getId(), family.getName(), family.getBaseCurrency(),
            role, memberRepository.findByFamilyId(principal).size());
    }

    @GetMapping("/{id}/members")
    public List<FamilyDto.MemberView> members(@PathVariable Long id) {
        return families.members(currentUser.require(), currentUser.requireFamily(id));
    }

    @PostMapping("/{id}/members/invite")
    public ResponseEntity<Void> invite(@PathVariable Long id, @Valid @RequestBody FamilyDto.InviteRequest request) {
        families.invite(currentUser.require(), currentUser.requireFamily(id), request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> changeRole(@PathVariable Long id, @PathVariable Long userId,
                                           @Valid @RequestBody FamilyDto.RoleRequest request) {
        families.changeRole(currentUser.require(), currentUser.requireFamily(id), userId, request.role());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> remove(@PathVariable Long id, @PathVariable Long userId) {
        families.remove(currentUser.require(), currentUser.requireFamily(id), userId);
        return ResponseEntity.noContent().build();
    }
}
