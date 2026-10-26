package vn.ptit.ffa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.ffa.domain.Budget;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByFamilyId(Long familyId);

    List<Budget> findByFamilyIdAndPeriodStart(Long familyId, LocalDate periodStart);

    Optional<Budget> findByFamilyIdAndCategoryIdAndPeriodStart(Long familyId, Long categoryId, LocalDate periodStart);

    boolean existsByCategoryId(Long categoryId);
}
