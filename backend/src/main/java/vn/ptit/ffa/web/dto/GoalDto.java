package vn.ptit.ffa.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTO muc tieu tiet kiem (DUAN-58). */
public final class GoalDto {

    private GoalDto() {
    }

    public record Request(@jakarta.validation.constraints.NotBlank @Size(max = 120) String name,
                          @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal targetAmount,
                          LocalDate deadline,
                          String status) {
    }

    public record ContributionRequest(@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
                                      @NotNull Long walletId,
                                      @NotNull LocalDate contributedOn) {
    }

    public record ContributionView(Long id, Long walletId, BigDecimal amount, LocalDate contributedOn) {
    }

    public record View(Long id,
                       Long familyId,
                       String name,
                       BigDecimal targetAmount,
                       BigDecimal savedAmount,
                       LocalDate deadline,
                       String status,
                       int percent,
                       List<ContributionView> contributions) {
    }
}
