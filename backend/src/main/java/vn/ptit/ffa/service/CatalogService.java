package vn.ptit.ffa.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.ffa.domain.Category;
import vn.ptit.ffa.domain.CategoryKind;
import vn.ptit.ffa.domain.Wallet;
import vn.ptit.ffa.domain.WalletType;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.repository.BudgetRepository;
import vn.ptit.ffa.repository.CategoryRepository;
import vn.ptit.ffa.repository.TransactionRepository;
import vn.ptit.ffa.repository.WalletRepository;
import vn.ptit.ffa.web.dto.CatalogDto;

import java.math.BigDecimal;
import java.util.List;

/** Danh muc va vi (DUAN-48, DUAN-49). Moi thao tác pham vi mot familyId. */
@Service
public class CatalogService {

    private final CategoryRepository categories;
    private final WalletRepository wallets;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;

    public CatalogService(CategoryRepository categories, WalletRepository wallets,
                          TransactionRepository transactions, BudgetRepository budgets) {
        this.categories = categories;
        this.wallets = wallets;
        this.transactions = transactions;
        this.budgets = budgets;
    }

    @Transactional(readOnly = true)
    public List<CatalogDto.CategoryView> listCategories(Long familyId, String kind) {
        List<Category> found = (kind == null || kind.isBlank())
            ? categories.findByFamilyIdOrderBySortOrderAscIdAsc(familyId)
            : categories.findByFamilyIdAndKindOrderBySortOrderAscIdAsc(familyId, parseKind(kind));
        return found.stream().map(this::view).toList();
    }

    @Transactional
    public CatalogDto.CategoryView create(Long familyId, CatalogDto.CategoryRequest request) {
        CategoryKind kind = parseKind(request.kind());
        requireUniqueName(familyId, kind, request.name(), null);
        if (request.parentId() != null) {
            Category parent = requireCategory(familyId, request.parentId());
            if (parent.getKind() != kind) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Danh mục cha phải cùng loại thu/chi.");
            }
        }
        Category entity = new Category();
        entity.setFamilyId(familyId);
        entity.setName(request.name().trim());
        entity.setKind(kind);
        entity.setParentId(request.parentId());
        entity.setIcon(request.icon());
        entity.setColor(request.color());
        entity.setSystem(false);
        entity.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return view(categories.save(entity));
    }

    @Transactional
    public CatalogDto.CategoryView update(Long familyId, Long id, CatalogDto.CategoryRequest request) {
        Category entity = requireCategory(familyId, id);
        if (entity.isSystem()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_ROLE",
                "Danh mục hệ thống không sửa tên hay màu được, hãy tạo danh mục riêng.");
        }
        CategoryKind kind = parseKind(request.kind());
        requireUniqueName(familyId, kind, request.name(), id);
        entity.setName(request.name().trim());
        entity.setKind(kind);
        if (request.parentId() != null && !request.parentId().equals(id)) {
            entity.setParentId(request.parentId());
        }
        entity.setIcon(request.icon());
        entity.setColor(request.color());
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        return view(categories.save(entity));
    }

    @Transactional
    public void delete(Long familyId, Long id) {
        Category entity = requireCategory(familyId, id);
        if (entity.isSystem()) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_IN_USE", "Không xoá được danh mục hệ thống.");
        }
        if (transactions.existsByCategoryIdAndDeletedAtIsNull(id) || budgets.existsByCategoryId(id)
            || categories.existsByParentId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_IN_USE",
                "Danh mục còn được giao dịch, ngân sách hoặc danh mục con tham chiếu.");
        }
        categories.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<CatalogDto.WalletView> listWallets(Long familyId, boolean includeArchived) {
        List<Wallet> found = includeArchived
            ? wallets.findByFamilyIdOrderByCreatedAtAscIdAsc(familyId)
            : wallets.findByFamilyIdAndArchivedFalseOrderByCreatedAtAscIdAsc(familyId);
        return found.stream().map(wallet -> new CatalogDto.WalletView(
            wallet.getId(), wallet.getName(), wallet.getType().name(), wallet.getCurrency(),
            wallet.getOpeningBalance(), balanceOf(familyId, wallet), wallet.isArchived())).toList();
    }

    @Transactional
    public CatalogDto.WalletView create(Long familyId, CatalogDto.WalletRequest request) {
        if (wallets.existsByFamilyIdAndName(familyId, request.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "WALLET_NAME_EXISTS",
                "Hộ đã có ví trùng tên này.");
        }
        Wallet wallet = new Wallet();
        wallet.setFamilyId(familyId);
        wallet.setName(request.name().trim());
        wallet.setType(parseType(request.type()));
        wallet.setCurrency(request.currency() == null || request.currency().isBlank() ? "VND" : request.currency().toUpperCase());
        wallet.setOpeningBalance(request.openingBalance());
        wallet.setArchived(false);
        wallets.save(wallet);
        return new CatalogDto.WalletView(wallet.getId(), wallet.getName(), wallet.getType().name(),
            wallet.getCurrency(), wallet.getOpeningBalance(), wallet.getOpeningBalance(), false);
    }

    @Transactional
    public CatalogDto.WalletView update(Long familyId, Long id, CatalogDto.WalletUpdate request) {
        Wallet wallet = requireWallet(familyId, id);
        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            if (!name.equals(wallet.getName()) && wallets.existsByFamilyIdAndName(familyId, name)) {
                throw new ApiException(HttpStatus.CONFLICT, "WALLET_NAME_EXISTS", "Tên ví đã tồn tại trong hộ.");
            }
            wallet.setName(name);
        }
        if (request.archived() != null) {
            wallet.setArchived(request.archived());
        }
        wallets.save(wallet);
        return new CatalogDto.WalletView(wallet.getId(), wallet.getName(), wallet.getType().name(),
            wallet.getCurrency(), wallet.getOpeningBalance(), balanceOf(familyId, wallet), wallet.isArchived());
    }

    @Transactional
    public void deleteWallet(Long familyId, Long id) {
        Wallet wallet = requireWallet(familyId, id);
        if (transactions.existsByWalletIdAndDeletedAtIsNull(id)
            || transactions.existsByTransferWalletIdAndDeletedAtIsNull(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "WALLET_IN_USE",
                "Ví còn giao dịch, chỉ ẩn (archive) được thôi.");
        }
        wallets.delete(wallet);
    }

    /** balance = so du dau + tien vo - tien ra (TRANSFER tinh 2 chieu). */
    private BigDecimal balanceOf(Long familyId, Wallet wallet) {
        BigDecimal opening = wallet.getOpeningBalance() == null ? BigDecimal.ZERO : wallet.getOpeningBalance();
        return opening
            .add(transactions.walletInflow(familyId, wallet.getId()))
            .subtract(transactions.walletOutflow(familyId, wallet.getId()));
    }

    private Category requireCategory(Long familyId, Long id) {
        Category entity = categories.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy danh mục."));
        if (!entity.getFamilyId().equals(familyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy danh mục.");
        }
        return entity;
    }

    private Wallet requireWallet(Long familyId, Long id) {
        Wallet wallet = wallets.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy ví."));
        if (!wallet.getFamilyId().equals(familyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy ví.");
        }
        return wallet;
    }

    private void requireUniqueName(Long familyId, CategoryKind kind, String name, Long excludeId) {
        categories.findByFamilyIdAndKindAndName(familyId, kind, name.trim())
            .filter(existing -> !existing.getId().equals(excludeId))
            .ifPresent(existing -> {
                throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_NAME_EXISTS",
                    "Danh mục trùng tên đã tồn tại trong hộ.");
            });
    }

    private CategoryKind parseKind(String value) {
        try {
            return CategoryKind.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "kind chỉ nhận INCOME hoặc EXPENSE.");
        }
    }

    private WalletType parseType(String value) {
        try {
            return WalletType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "type ví không hợp lệ (CASH, BANK, E_WALLET, CREDIT_CARD, SAVINGS).");
        }
    }

    private CatalogDto.CategoryView view(Category entity) {
        return new CatalogDto.CategoryView(entity.getId(), entity.getParentId(), entity.getName(),
            entity.getKind().name(), entity.getIcon(), entity.getColor(), entity.isSystem(), entity.getSortOrder());
    }
}
