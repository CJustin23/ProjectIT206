package vn.ptit.ffa.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.ffa.domain.Category;
import vn.ptit.ffa.domain.CategoryKind;
import vn.ptit.ffa.domain.TransactionType;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.repository.CategoryRepository;
import vn.ptit.ffa.repository.TransactionRepository;
import vn.ptit.ffa.security.AuthPrincipal;
import vn.ptit.ffa.web.dto.CatalogDto;
import vn.ptit.ffa.web.dto.ReportDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tổng hợp thu chi (DUAN-54, DUAN-55). Kết quả được cache theo (familyId + kỳ)
 * vì số liệu trong ngày gần như không đổi (DUAN-57).
 */
@Service
public class ReportService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final CatalogService catalog;
    private final BudgetService budgetService;

    public ReportService(TransactionRepository transactions, CategoryRepository categories,
                         CatalogService catalog, BudgetService budgetService) {
        this.transactions = transactions;
        this.categories = categories;
        this.catalog = catalog;
        this.budgetService = budgetService;
    }

    @Cacheable(value = "dashboard", key = "#principal.familyId() + ':' + #month")
    @Transactional(readOnly = true)
    public ReportDto.DashboardView dashboard(AuthPrincipal principal, String month) {
        YearMonth period = parseMonth(month);
        LocalDate from = period.atDay(1);
        LocalDate to = period.atEndOfMonth();

        BigDecimal income = nz(transactions.sumByType(principal.familyId(), TransactionType.INCOME, from, to));
        BigDecimal expense = nz(transactions.sumByType(principal.familyId(), TransactionType.EXPENSE, from, to));
        long count = transactions
            .search(principal.familyId(), null, null, null, from, to, PageRequest.of(0, 1))
            .getTotalElements();

        return new ReportDto.DashboardView(
            period.toString(), income, expense, income.subtract(expense), count,
            catalog.listWallets(principal.familyId(), false),
            byCategory(principal, from, to, CategoryKind.EXPENSE.name(), 8),
            trend(principal, period),
            budgetService.alerts(principal, from));
    }

    @Cacheable(value = "reports", key = "'year:' + #principal.familyId() + ':' + #year")
    @Transactional(readOnly = true)
    public ReportDto.YearlyView yearly(AuthPrincipal principal, int year) {
        List<ReportDto.MonthTotalsView> months = monthsOfYear(principal.familyId(), year);
        BigDecimal income = months.stream().map(ReportDto.MonthTotalsView::income).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = months.stream().map(ReportDto.MonthTotalsView::expense).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportDto.YearlyView(year, months, income, expense, income.subtract(expense));
    }

    @Transactional(readOnly = true)
    public List<ReportDto.CategoryShareView> byCategory(AuthPrincipal principal, LocalDate from, LocalDate to,
                                                        String kind, int limit) {
        CategoryKind categoryKind = parseKind(kind);
        TransactionType type = categoryKind == CategoryKind.INCOME ? TransactionType.INCOME : TransactionType.EXPENSE;
        var rows = transactions.totalsByCategory(principal.familyId(), type, from, to);
        int max = limit <= 0 ? 10 : limit;
        BigDecimal total = rows.stream().map(row -> nz(row.getTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ReportDto.CategoryShareView> result = new ArrayList<>();
        for (var row : rows.stream().limit(max).toList()) {
            BigDecimal rowTotal = nz(row.getTotal());
            BigDecimal percent = total.signum() == 0
                ? BigDecimal.ZERO
                : rowTotal.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
            String name = categories.findById(row.getCategoryId()).map(Category::getName).orElse("Khác");
            result.add(new ReportDto.CategoryShareView(row.getCategoryId(), name, categoryKind.name(), rowTotal, percent));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReportDto.WalletFlowView> byWallet(AuthPrincipal principal, LocalDate from, LocalDate to) {
        Map<Long, CatalogDto.WalletView> byId = new HashMap<>();
        catalog.listWallets(principal.familyId(), true).forEach(wallet -> byId.put(wallet.id(), wallet));

        return transactions.totalsByWallet(principal.familyId(), from, to).stream()
            .map(row -> {
                CatalogDto.WalletView wallet = byId.get(row.getWalletId());
                return new ReportDto.WalletFlowView(row.getWalletId(),
                    wallet == null ? "Ví #" + row.getWalletId() : wallet.name(),
                    wallet == null ? null : wallet.type(),
                    nz(row.getTotal()));
            })
            .toList();
    }

    private List<ReportDto.MonthTotalsView> trend(AuthPrincipal principal, YearMonth current) {
        Map<String, ReportDto.MonthTotalsView> byPeriod = new HashMap<>();
        for (int year : new int[] {current.getYear(), current.getYear() - 1}) {
            monthsOfYear(principal.familyId(), year).forEach(row -> byPeriod.put(row.month(), row));
        }
        List<ReportDto.MonthTotalsView> trend = new ArrayList<>(12);
        for (int back = 11; back >= 0; back--) {
            YearMonth month = current.minusMonths(back);
            String key = formatMonth(month);
            trend.add(byPeriod.getOrDefault(key,
                new ReportDto.MonthTotalsView(key, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        }
        return trend;
    }

    private List<ReportDto.MonthTotalsView> monthsOfYear(Long familyId, int year) {
        return transactions.monthlyTotals(familyId, year).stream()
            .map(row -> {
                BigDecimal income = nz(row.getIncome());
                BigDecimal expense = nz(row.getExpense());
                return new ReportDto.MonthTotalsView(
                    formatMonth(YearMonth.of(row.getYear(), row.getMonth())), income, expense,
                    income.subtract(expense));
            })
            .toList();
    }

    private String formatMonth(YearMonth month) {
        return String.format("%04d-%02d", month.getYear(), month.getMonthValue());
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Tham số month phải theo dạng YYYY-MM.");
        }
    }

    private CategoryKind parseKind(String kind) {
        if (kind == null || kind.isBlank()) {
            return CategoryKind.EXPENSE;
        }
        try {
            return CategoryKind.valueOf(kind.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "kind chỉ nhận INCOME hoặc EXPENSE.");
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
