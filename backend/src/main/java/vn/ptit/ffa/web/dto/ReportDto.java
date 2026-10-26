package vn.ptit.ffa.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** DTO bao cao thang / nam / theo danh muc va vi (DUAN-54, DUAN-55). */
public final class ReportDto {

    private ReportDto() {
    }

    public record MonthTotalsView(String month,
                                  BigDecimal income,
                                  BigDecimal expense,
                                  BigDecimal net) {
    }

    public record CategoryShareView(Long categoryId,
                                    String name,
                                    String kind,
                                    BigDecimal total,
                                    BigDecimal percent) {
    }

    public record WalletFlowView(Long walletId,
                                 String name,
                                 String type,
                                 BigDecimal total) {
    }

    public record DashboardView(String month,
                                BigDecimal income,
                                BigDecimal expense,
                                BigDecimal net,
                                long transactionCount,
                                List<CatalogDto.WalletView> wallets,
                                List<CategoryShareView> topCategories,
                                List<MonthTotalsView> trend12Months,
                                List<BudgetDto.View> budgetAlerts) {
    }

    public record YearlyView(int year,
                             List<MonthTotalsView> months,
                             BigDecimal totalIncome,
                             BigDecimal totalExpense,
                             BigDecimal net) {
    }
}
