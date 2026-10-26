package vn.ptit.ffa.repository;

import java.math.BigDecimal;

/** Typed projection for the monthly income/expense summary (BR-10). */
public interface MonthlyTotalProjection {
    Integer getYear();

    Integer getMonth();

    BigDecimal getIncome();

    BigDecimal getExpense();
}
