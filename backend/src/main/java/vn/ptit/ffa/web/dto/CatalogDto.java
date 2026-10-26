package vn.ptit.ffa.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** DTO cho danh muc va vi (DUAN-48, DUAN-49). */
public final class CatalogDto {

    private CatalogDto() {
    }

    public record CategoryView(Long id,
                               Long parentId,
                               String name,
                               String kind,
                               String icon,
                               String color,
                               boolean system,
                               Integer sortOrder) {
    }

    public record CategoryRequest(@NotBlank @Size(max = 100) String name,
                                  @NotBlank String kind,
                                  Long parentId,
                                  @Size(max = 40) String icon,
                                  @Size(max = 7) String color,
                                  Integer sortOrder) {
    }

    public record WalletView(Long id,
                             String name,
                             String type,
                             String currency,
                             BigDecimal openingBalance,
                             BigDecimal balance,
                             boolean archived) {
    }

    public record WalletRequest(@NotBlank @Size(max = 100) String name,
                                @NotBlank String type,
                                @Size(min = 3, max = 3) String currency,
                                @NotNull @PositiveOrZero BigDecimal openingBalance) {
    }

    public record WalletUpdate(String name, Boolean archived) {
    }
}
