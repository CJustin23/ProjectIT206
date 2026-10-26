package vn.ptit.ffa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ptit.ffa.domain.GoalContribution;

import java.math.BigDecimal;
import java.util.List;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    List<GoalContribution> findByGoalIdOrderByContributedOnDescIdDesc(Long goalId);

    @Query("SELECT COALESCE(SUM(g.amount), 0) FROM GoalContribution g WHERE g.goalId = :goalId")
    BigDecimal sumAmountByGoalId(@Param("goalId") Long goalId);
}
