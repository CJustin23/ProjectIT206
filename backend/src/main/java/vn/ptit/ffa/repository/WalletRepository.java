package vn.ptit.ffa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.ffa.domain.Wallet;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByFamilyIdOrderByCreatedAtAscIdAsc(Long familyId);

    List<Wallet> findByFamilyIdAndArchivedFalseOrderByCreatedAtAscIdAsc(Long familyId);

    Optional<Wallet> findByFamilyIdAndName(Long familyId, String name);

    boolean existsByFamilyIdAndName(Long familyId, String name);
}
