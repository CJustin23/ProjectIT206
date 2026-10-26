package vn.ptit.ffa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.ffa.domain.Family;

public interface FamilyRepository extends JpaRepository<Family, Long> {

    boolean existsByName(String name);
}
