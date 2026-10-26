package vn.ptit.ffa.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO ngan sach (DUAN-53, DUAN-55). */
public final class BudgetDto {

    private BudgetDto() {
    }

    public record Request(@NotNull Long categoryId,
                          Long walletId,
                          @NotNull LocalDate periodStart,
                          @NotNull LocalDate periodEnd,
                          @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal limitAmount,
                          Integer alertThresholdPercent,
                          Long version) {
    }

    public record View(Long id,
                       Long categoryId,
                       String categoryName,
                       Long walletId,
                       LocalDate periodStart,
                       LocalDate periodEnd,
                       BigDecimal limitAmount,
                       BigDecimal used,
                       BigDecimal remaining,
                       int percent,
                       String status,
                       int alertThresholdPercent,
                       Long version) {
    }
}
