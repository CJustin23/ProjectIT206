package vn.ptit.ffa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.ffa.domain.GoalStatus;
import vn.ptit.ffa.domain.SavingsGoal;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByFamilyId(Long familyId);

    List<SavingsGoal> findByFamilyIdAndStatus(Long familyId, GoalStatus status);
}
