package vn.ptit.ffa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.ptit.ffa.domain.Transaction;
import vn.ptit.ffa.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Filtered, paged listing (api.md section 2.4). Every predicate is tenant-scoped by familyId
     * and excludes soft-deleted rows (BR-05, BR-09). Optional filters pass null to be ignored.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.familyId = :familyId
              AND t.deletedAt IS NULL
              AND (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.categoryId = :categoryId)
              AND (:walletId IS NULL OR t.walletId = :walletId)
              AND (:from IS NULL OR t.transactionDate >= :from)
              AND (:to IS NULL OR t.transactionDate <= :to)
            ORDER BY t.transactionDate DESC, t.id DESC
            """)
    Page<Transaction> search(@Param("familyId") Long familyId,
                             @Param("type") TransactionType type,
                             @Param("categoryId") Long categoryId,
                             @Param("walletId") Long walletId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to,
                             Pageable pageable);

    /** Giống search() nhưng lọc thêm theo ghi chú (bỏ hoa thường), phục vụ ô tìm kiếm của FE. */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.familyId = :familyId
              AND t.deletedAt IS NULL
              AND (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.categoryId = :categoryId)
              AND (:walletId IS NULL OR t.walletId = :walletId)
              AND (:from IS NULL OR t.transactionDate >= :from)
              AND (:to IS NULL OR t.transactionDate <= :to)
              AND LOWER(COALESCE(t.note, '')) LIKE CONCAT('%', :keyword, '%')
            ORDER BY t.transactionDate DESC, t.id DESC
            """)
    Page<Transaction> searchKeyword(@Param("familyId") Long familyId,
                                    @Param("type") TransactionType type,
                                    @Param("categoryId") Long categoryId,
                                    @Param("walletId") Long walletId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.familyId = :familyId
              AND t.deletedAt IS NULL
              AND t.type = :type
              AND (:from IS NULL OR t.transactionDate >= :from)
              AND (:to IS NULL OR t.transactionDate <= :to)
            """)
    BigDecimal sumByType(@Param("familyId") Long familyId,
                         @Param("type") TransactionType type,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);

    /**
     * Tien ra khoi vi: chi tieu + chuyen khoan i (walletId la vi gui).
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.deletedAt IS NULL AND t.familyId = :familyId AND t.walletId = :walletId
              AND t.type IN (vn.ptit.ffa.domain.TransactionType.EXPENSE,
                             vn.ptit.ffa.domain.TransactionType.TRANSFER)
            """)
    BigDecimal walletOutflow(@Param("familyId") Long familyId, @Param("walletId") Long walletId);

    /**
     * Tien vo vi: thu + chuyen khoan den (transferWalletId la vi nhan).
     * balance = openingBalance + walletInflow - walletOutflow.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.deletedAt IS NULL AND t.familyId = :familyId
              AND ((t.type = vn.ptit.ffa.domain.TransactionType.INCOME AND t.walletId = :walletId)
                OR (t.type = vn.ptit.ffa.domain.TransactionType.TRANSFER AND t.transferWalletId = :walletId))
            """)
    BigDecimal walletInflow(@Param("familyId") Long familyId, @Param("walletId") Long walletId);

    @Query("""
            SELECT t.categoryId AS categoryId, SUM(t.amount) AS total FROM Transaction t
            WHERE t.familyId = :familyId
              AND t.deletedAt IS NULL
              AND t.type = :type
              AND t.categoryId IS NOT NULL
              AND (:from IS NULL OR t.transactionDate >= :from)
              AND (:to IS NULL OR t.transactionDate <= :to)
            GROUP BY t.categoryId
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryTotalProjection> totalsByCategory(@Param("familyId") Long familyId,
                                                   @Param("type") TransactionType type,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Query("""
            SELECT t.walletId AS walletId, SUM(t.amount) AS total FROM Transaction t
            WHERE t.familyId = :familyId
              AND t.deletedAt IS NULL
              AND (:from IS NULL OR t.transactionDate >= :from)
              AND (:to IS NULL OR t.transactionDate <= :to)
            GROUP BY t.walletId
            """)
    List<WalletTotalProjection> totalsByWallet(@Param("familyId") Long familyId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    @Query("""
            SELECT YEAR(t.transactionDate) AS year,
                   MONTH(t.transactionDate) AS month,
                   COALESCE(SUM(CASE WHEN t.type = vn.ptit.ffa.domain.TransactionType.INCOME
                                     THEN t.amount ELSE 0 END), 0) AS income,
                   COALESCE(SUM(CASE WHEN t.type = vn.ptit.ffa.domain.TransactionType.EXPENSE
                                     THEN t.amount ELSE 0 END), 0) AS expense
            FROM Transaction t
            WHERE t.familyId = :familyId
              AND t.deletedAt IS NULL
              AND FUNCTION('YEAR', t.transactionDate) = :year
            GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate)
            ORDER BY MONTH(t.transactionDate) ASC
            """)
    List<MonthlyTotalProjection> monthlyTotals(@Param("familyId") Long familyId,
                                               @Param("year") Integer year);

    boolean existsByCategoryIdAndDeletedAtIsNull(Long categoryId);

    /** Số tiền đã chi của một danh mục trong kỳ ngân sách (DUAN-53). */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.familyId = :familyId AND t.deletedAt IS NULL
              AND t.type = vn.ptit.ffa.domain.TransactionType.EXPENSE
              AND t.categoryId = :categoryId
              AND t.transactionDate >= :from AND t.transactionDate <= :to
            """)
    BigDecimal spentInCategoryPeriod(@Param("familyId") Long familyId,
                                     @Param("categoryId") Long categoryId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    boolean existsByWalletIdAndDeletedAtIsNull(Long walletId);

    boolean existsByTransferWalletIdAndDeletedAtIsNull(Long walletId);
}
