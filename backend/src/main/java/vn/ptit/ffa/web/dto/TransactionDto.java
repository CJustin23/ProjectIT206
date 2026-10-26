package vn.ptit.ffa.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** DTO cua phan he giao dich (DUAN-50). */
public final class TransactionDto {

    private TransactionDto() {
    }

    public record CreateRequest(@NotNull Long walletId,
                                Long categoryId,
                                Long transferWalletId,
                                @NotBlank String type,
                                @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
                                @NotNull LocalDate transactionDate,
                                @Size(max = 255) String note,
                                Boolean recurring) {
    }

    public record UpdateRequest(@NotNull Long walletId,
                                Long categoryId,
                                Long transferWalletId,
                                @NotBlank String type,
                                @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
                                @NotNull LocalDate transactionDate,
                                @Size(max = 255) String note,
                                Boolean recurring,
                                Long version) {
    }

    public record PersonView(Long id, String fullName) {
    }

    public record View(Long id,
                       Long familyId,
                       Long walletId,
                       Long categoryId,
                       Long transferWalletId,
                       String type,
                       BigDecimal amount,
                       LocalDate transactionDate,
                       String note,
                       boolean recurring,
                       PersonView createdBy,
                       BigDecimal walletBalanceAfter,
                       OffsetDateTime createdAt,
                       Long version) {
    }

    /** Phân trang theo quy ước api.md: content + _page + _size + _total. */
    public record PageView<T>(java.util.List<T> content,
                              int _page,
                              int _size,
                              long _total,
                              int totalPages) {
    }
}
