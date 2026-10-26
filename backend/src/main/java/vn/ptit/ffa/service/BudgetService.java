package vn.ptit.ffa.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.ffa.domain.Budget;
import vn.ptit.ffa.domain.Category;
import vn.ptit.ffa.domain.CategoryKind;
import vn.ptit.ffa.domain.MemberRole;
import vn.ptit.ffa.domain.Notification;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.repository.BudgetRepository;
import vn.ptit.ffa.repository.CategoryRepository;
import vn.ptit.ffa.repository.FamilyMemberRepository;
import vn.ptit.ffa.repository.NotificationRepository;
import vn.ptit.ffa.repository.TransactionRepository;
import vn.ptit.ffa.security.AuthPrincipal;
import vn.ptit.ffa.web.dto.BudgetDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** Ngân sách theo danh mục và kỳ (DUAN-53) + sinh cảnh báo vượt ngưỡng (DUAN-55). */
@Service
public class BudgetService {

    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final NotificationRepository notifications;
    private final FamilyMemberRepository members;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories,
                         TransactionRepository transactions, NotificationRepository notifications,
                         FamilyMemberRepository members) {
        this.budgets = budgets;
        this.categories = categories;
        this.transactions = transactions;
        this.notifications = notifications;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public List<BudgetDto.View> list(AuthPrincipal principal, LocalDate periodStart) {
        List<Budget> found = periodStart == null
            ? budgets.findByFamilyId(principal.familyId())
            : budgets.findByFamilyIdAndPeriodStart(principal.familyId(), periodStart);
        return found.stream().map(budget -> view(principal.familyId(), budget)).toList();
    }

    @Transactional
    public BudgetDto.View upsert(AuthPrincipal principal, BudgetDto.Request request) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Ngày kết thúc kỳ phải sau ngày bắt đầu.");
        }
        Category category = categories.findById(request.categoryId())
            .filter(entity -> entity.getFamilyId().equals(principal.familyId()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy danh mục."));
        if (category.getKind() != CategoryKind.EXPENSE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Ngân sách chỉ đặt cho danh mục chi.");
        }

        Budget budget = budgets
            .findByFamilyIdAndCategoryIdAndPeriodStart(principal.familyId(), category.getId(), request.periodStart())
            .orElseGet(Budget::new);

        if (budget.getId() != null && request.version() != null && !request.version().equals(budget.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "BUDGET_CONFLICT",
                "Ngân sách vừa được người khác sửa, tải lại rồi thử lại.");
        }

        budget.setFamilyId(principal.familyId());
        budget.setCategoryId(category.getId());
        budget.setWalletId(request.walletId());
        budget.setPeriodStart(request.periodStart());
        budget.setPeriodEnd(request.periodEnd());
        budget.setLimitAmount(request.limitAmount());
        budget.setAlertThresholdPercent(request.alertThresholdPercent() == null ? 80 : request.alertThresholdPercent());

        try {
            budgets.save(budget);
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "BUDGET_CONFLICT",
                "Danh mục này đã có ngân sách cho kỳ bắt đầu " + request.periodStart() + ".");
        }
        BudgetDto.View view = view(principal.familyId(), budget);
        raiseAlertIfNeeded(principal, category, budget, view);
        return view;
    }

    @Transactional
    public void delete(AuthPrincipal principal, Long id) {
        Budget budget = budgets.findById(id)
            .filter(entity -> entity.getFamilyId().equals(principal.familyId()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy ngân sách."));
        budgets.delete(budget);
    }

    /** Danh sách đang ở ngưỡng cảnh báo — Dashboard dùng (DUAN-55). */
    @Transactional(readOnly = true)
    public List<BudgetDto.View> alerts(AuthPrincipal principal, LocalDate periodStart) {
        return list(principal, periodStart).stream()
            .filter(view -> !"SAFE".equals(view.status()))
            .toList();
    }

    private BudgetDto.View view(Long familyId, Budget budget) {
        BigDecimal used = transactions.spentInCategoryPeriod(familyId, budget.getCategoryId(),
            budget.getPeriodStart(), budget.getPeriodEnd());
        int percent = budget.getLimitAmount().signum() == 0
            ? 0
            : used.multiply(BigDecimal.valueOf(100))
                .divide(budget.getLimitAmount(), 0, RoundingMode.DOWN)
                .intValue();
        String status = percent >= 100 ? "OVER" : percent >= budget.getAlertThresholdPercent() ? "WARNING" : "SAFE";
        String name = categories.findById(budget.getCategoryId()).map(Category::getName).orElse("");
        return new BudgetDto.View(budget.getId(), budget.getCategoryId(), name, budget.getWalletId(),
            budget.getPeriodStart(), budget.getPeriodEnd(), budget.getLimitAmount(), used,
            budget.getLimitAmount().subtract(used), percent, status,
            budget.getAlertThresholdPercent(), budget.getVersion());
    }

    private void raiseAlertIfNeeded(AuthPrincipal principal, Category category, Budget budget, BudgetDto.View view) {
        if ("SAFE".equals(view.status())) {
            return;
        }
        String title = "OVER".equals(view.status())
            ? "Vượt ngân sách " + category.getName()
            : "Sắp chạm ngân sách " + category.getName();
        String body = "Đã chi " + view.used() + " / " + budget.getLimitAmount() + " (" + view.percent() + "%).";
        members.findByFamilyId(budget.getFamilyId()).stream()
            .filter(membership -> membership.getRole() != MemberRole.VIEWER)
            .forEach(membership -> notifications.save(
                Notification.budgetAlert(membership.getUserId(), budget.getFamilyId(), title, body)));
    }
}
