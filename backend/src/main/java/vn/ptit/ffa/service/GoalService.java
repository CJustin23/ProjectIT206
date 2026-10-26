package vn.ptit.ffa.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.ffa.domain.GoalContribution;
import vn.ptit.ffa.domain.GoalStatus;
import vn.ptit.ffa.domain.SavingsGoal;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.repository.GoalContributionRepository;
import vn.ptit.ffa.repository.SavingsGoalRepository;
import vn.ptit.ffa.repository.WalletRepository;
import vn.ptit.ffa.security.AuthPrincipal;
import vn.ptit.ffa.web.dto.GoalDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Mục tiêu tiết kiệm và tiến độ tích luỹ (DUAN-58). */
@Service
public class GoalService {

    private final SavingsGoalRepository goals;
    private final GoalContributionRepository contributions;
    private final WalletRepository wallets;

    public GoalService(SavingsGoalRepository goals, GoalContributionRepository contributions, WalletRepository wallets) {
        this.goals = goals;
        this.contributions = contributions;
        this.wallets = wallets;
    }

    @Transactional(readOnly = true)
    public List<GoalDto.View> list(AuthPrincipal principal, String status) {
        List<SavingsGoal> found = status == null || status.isBlank()
            ? goals.findByFamilyId(principal.familyId())
            : goals.findByFamilyIdAndStatus(principal.familyId(), GoalStatus.valueOf(status.toUpperCase()));
        return found.stream().map(this::view).toList();
    }

    @Transactional
    public GoalDto.View create(AuthPrincipal principal, GoalDto.Request request) {
        SavingsGoal goal = new SavingsGoal();
        goal.setFamilyId(principal.familyId());
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setSavedAmount(BigDecimal.ZERO);
        goal.setDeadline(request.deadline());
        goal.setStatus(GoalStatus.ACTIVE);
        goals.save(goal);
        return view(goal);
    }

    @Transactional
    public GoalDto.View update(AuthPrincipal principal, Long id, GoalDto.Request request) {
        SavingsGoal goal = require(principal.familyId(), id);
        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setDeadline(request.deadline());
        if (request.status() != null && !request.status().isBlank()) {
            goal.setStatus(GoalStatus.valueOf(request.status().toUpperCase()));
        }
        goals.save(goal);
        return view(goal);
    }

    @Transactional
    public void delete(AuthPrincipal principal, Long id) {
        goals.delete(require(principal.familyId(), id));
    }

    @Transactional
    public GoalDto.View contribute(AuthPrincipal principal, Long id, GoalDto.ContributionRequest request) {
        SavingsGoal goal = require(principal.familyId(), id);
        wallets.findById(request.walletId())
            .filter(wallet -> wallet.getFamilyId().equals(principal.familyId()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy ví nguồn."));
        if (goal.getStatus() == GoalStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GOAL_CANCELLED", "Mục tiêu đã huỷ, không nạp thêm được.");
        }

        GoalContribution contribution = new GoalContribution();
        contribution.setGoalId(goal.getId());
        contribution.setWalletId(request.walletId());
        contribution.setAmount(request.amount());
        contribution.setContributedOn(request.contributedOn());
        contribution.setCreatedBy(principal.userId());
        contributions.save(contribution);

        goal.setSavedAmount(contributions.sumAmountByGoalId(goal.getId()));
        if (goal.getSavedAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        }
        goals.save(goal);
        return view(goal);
    }

    private SavingsGoal require(Long familyId, Long id) {
        SavingsGoal goal = goals.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy mục tiêu."));
        if (!goal.getFamilyId().equals(familyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy mục tiêu.");
        }
        return goal;
    }

    private GoalDto.View view(SavingsGoal goal) {
        int percent = goal.getTargetAmount().signum() == 0
            ? 0
            : goal.getSavedAmount().multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 0, RoundingMode.DOWN).intValue();
        List<GoalDto.ContributionView> history = contributions.findByGoalIdOrderByContributedOnDescIdDesc(goal.getId())
            .stream()
            .map(row -> new GoalDto.ContributionView(row.getId(), row.getWalletId(), row.getAmount(), row.getContributedOn()))
            .toList();
        return new GoalDto.View(goal.getId(), goal.getFamilyId(), goal.getName(), goal.getTargetAmount(),
            goal.getSavedAmount(), goal.getDeadline(), goal.getStatus().name(), percent, history);
    }
}
