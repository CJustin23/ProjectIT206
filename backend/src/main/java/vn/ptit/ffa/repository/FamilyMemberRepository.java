package vn.ptit.ffa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.ffa.domain.FamilyMember;

import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findByUserId(Long userId);

    List<FamilyMember> findByFamilyId(Long familyId);

    Optional<FamilyMember> findByFamilyIdAndUserId(Long familyId, Long userId);

    boolean existsByFamilyIdAndUserId(Long familyId, Long userId);

    void deleteByFamilyIdAndUserId(Long familyId, Long userId);
}
