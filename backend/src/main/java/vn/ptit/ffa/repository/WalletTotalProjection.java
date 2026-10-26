package vn.ptit.ffa.repository;

import java.math.BigDecimal;

/** Typed projection for flow grouped by wallet (BR-09). */
public interface WalletTotalProjection {
    Long getWalletId();

    BigDecimal getTotal();
}
