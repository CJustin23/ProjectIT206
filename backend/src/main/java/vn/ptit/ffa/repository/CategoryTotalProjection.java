package vn.ptit.ffa.repository;

import java.math.BigDecimal;

/** Typed projection for spend/receipt grouped by category (BR-09). */
public interface CategoryTotalProjection {
    Long getCategoryId();

    BigDecimal getTotal();
}
