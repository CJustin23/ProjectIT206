package vn.ptit.ffa.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.ffa.domain.Category;
import vn.ptit.ffa.domain.CategoryKind;
import vn.ptit.ffa.domain.Transaction;
import vn.ptit.ffa.domain.TransactionType;
import vn.ptit.ffa.domain.User;
import vn.ptit.ffa.domain.Wallet;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.repository.CategoryRepository;
import vn.ptit.ffa.repository.TransactionRepository;
import vn.ptit.ffa.repository.UserRepository;
import vn.ptit.ffa.repository.WalletRepository;
import vn.ptit.ffa.security.AuthPrincipal;
import vn.ptit.ffa.web.dto.TransactionDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Giao dịch thu/chi/chuyển khoản (DUAN-50). Một lần ghi chỉ tạo đúng một dòng
 * transactions và cam ket rang no khong pha vo bat ky quy tac nao cua BR-05..BR-09.
 */
@Service
public class TransactionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TransactionRepository transactions;
    private final WalletRepository wallets;
    private final CategoryRepository categories;
    private final UserRepository users;

    public TransactionService(TransactionRepository transactions, WalletRepository wallets,
                              CategoryRepository categories, UserRepository users) {
        this.transactions = transactions;
        this.wallets = wallets;
        this.categories = categories;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public TransactionDto.PageView<TransactionDto.View> search(AuthPrincipal principal,
                                                               TransactionType type,
                                                               Long categoryId,
                                                               Long walletId,
                                                               LocalDate from,
                                                               LocalDate to,
                                                               String keyword,
                                                               int page,
                                                               int size) {
        Page<Transaction> result = keyword == null || keyword.isBlank()
            ? transactions.search(principal.familyId(), type, categoryId, walletId, from, to,
                PageRequest.of(page, Math.min(size, 100), Sort.by("transactionDate").descending()))
            : transactions.searchKeyword(principal.familyId(), type, categoryId, walletId, from, to,
                keyword.trim().toLowerCase(),
                PageRequest.of(page, Math.min(size, 100), Sort.by("transactionDate").descending()));

        List<TransactionDto.View> content = result.getContent().stream().map(this::view).toList();
        return new TransactionDto.PageView<>(content, result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TransactionDto.View get(AuthPrincipal principal, Long id) {
        return view(require(principal.familyId(), id));
    }

    @Transactional
    @CacheEvict(cacheNames = {"reports", "dashboard"}, allEntries = true)
    public TransactionDto.View create(AuthPrincipal principal, TransactionDto.CreateRequest request) {
        TransactionType type = parseType(request.type());
        Wallet wallet = requireWallet(principal.familyId(), request.walletId());
        Category category = resolveCategory(principal.familyId(), request.categoryId(), type);
        Wallet target = resolveTransferTarget(principal.familyId(), type, request.transferWalletId(), wallet);

        Transaction entity = new Transaction();
        entity.setFamilyId(principal.familyId());
        entity.setWalletId(wallet.getId());
        entity.setCategoryId(category == null ? null : category.getId());
        entity.setTransferWalletId(target == null ? null : target.getId());
        entity.setType(type);
        entity.setAmount(request.amount());
        entity.setTransactionDate(request.transactionDate());
        entity.setNote(request.note());
        entity.setRecurring(Boolean.TRUE.equals(request.recurring()));
        entity.setCreatedBy(principal.userId());
        transactions.save(entity);
        return view(entity);
    }

    @Transactional
    @CacheEvict(cacheNames = {"reports", "dashboard"}, allEntries = true)
    public TransactionDto.View update(AuthPrincipal principal, Long id, TransactionDto.UpdateRequest request) {
        Transaction entity = require(principal.familyId(), id);
        if (request.version() != null && !request.version().equals(entity.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "BUDGET_CONFLICT",
                "Bản ghi đã được người khác sửa, tải lại rồi thử lại.");
        }
        TransactionType type = parseType(request.type());
        Wallet wallet = requireWallet(principal.familyId(), request.walletId());
        Category category = resolveCategory(principal.familyId(), request.categoryId(), type);
        Wallet target = resolveTransferTarget(principal.familyId(), type, request.transferWalletId(), wallet);

        entity.setWalletId(wallet.getId());
        entity.setCategoryId(category == null ? null : category.getId());
        entity.setTransferWalletId(target == null ? null : target.getId());
        entity.setType(type);
        entity.setAmount(request.amount());
        entity.setTransactionDate(request.transactionDate());
        entity.setNote(request.note());
        entity.setRecurring(Boolean.TRUE.equals(request.recurring()));
        try {
            transactions.saveAndFlush(entity);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ApiException(HttpStatus.CONFLICT, "BUDGET_CONFLICT",
                "Bản ghi đã được người khác sửa, tải lại rồi thử lại.");
        }
        return view(entity);
    }

    /** BR-09: xoá mềm, giữ dấu vết để báo cáo quá khứ không đổi số liệu. */
    @Transactional
    @CacheEvict(cacheNames = {"reports", "dashboard"}, allEntries = true)
    public void delete(AuthPrincipal principal, Long id) {
        Transaction entity = require(principal.familyId(), id);
        entity.setDeletedAt(java.time.LocalDateTime.now());
        transactions.save(entity);
    }

    @Transactional(readOnly = true)
    public String exportCsv(AuthPrincipal principal, LocalDate from, LocalDate to) {
        StringBuilder csv = new StringBuilder("id,ngay,loai,danh_muc,vi,so_tien,ghi_chu\n");
        transactions.search(principal.familyId(), null, null, null, from, to,
                PageRequest.of(0, 10_000, Sort.by("transactionDate").descending()))
            .forEach(row -> csv.append(row.getId()).append(',')
                .append(row.getTransactionDate()).append(',')
                .append(row.getType()).append(',')
                .append(row.getCategoryId() == null ? "" : row.getCategoryId()).append(',')
                .append(row.getWalletId()).append(',')
                .append(row.getAmount().toPlainString()).append(',')
                .append(escape(row.getNote())).append('\n'));
        return csv.toString();
    }

    private Transaction require(Long familyId, Long id) {
        Transaction entity = transactions.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy giao dịch."));
        if (!entity.getFamilyId().equals(familyId) || entity.getDeletedAt() != null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy giao dịch.");
        }
        return entity;
    }

    private TransactionDto.View view(Transaction entity) {
        User creator = users.findById(entity.getCreatedBy()).orElse(null);
        BigDecimal balance = balanceOf(entity.getFamilyId(), entity.getWalletId());
        return new TransactionDto.View(
            entity.getId(), entity.getFamilyId(), entity.getWalletId(), entity.getCategoryId(),
            entity.getTransferWalletId(), entity.getType().name(), entity.getAmount(),
            entity.getTransactionDate(), entity.getNote(), entity.isRecurring(),
            creator == null ? null : new TransactionDto.PersonView(creator.getId(), creator.getFullName()),
            balance,
            entity.getCreatedAt() == null ? null : entity.getCreatedAt().atZone(BUSINESS_ZONE).toOffsetDateTime(),
            entity.getVersion());
    }

    private BigDecimal balanceOf(Long familyId, Long walletId) {
        Wallet wallet = wallets.findById(walletId).orElse(null);
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal opening = wallet.getOpeningBalance() == null ? BigDecimal.ZERO : wallet.getOpeningBalance();
        return opening.add(transactions.walletInflow(familyId, walletId))
            .subtract(transactions.walletOutflow(familyId, walletId));
    }

    private Wallet requireWallet(Long familyId, Long walletId) {
        Wallet wallet = wallets.findById(walletId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy ví."));
        if (!wallet.getFamilyId().equals(familyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy ví.");
        }
        if (wallet.isArchived()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Ví đã ẩn, không ghi được giao dịch.");
        }
        return wallet;
    }

    /** BR-05: danh mục phải đúng loại với giao dịch; TRANSFER không dùng danh mục. */
    private Category resolveCategory(Long familyId, Long categoryId, TransactionType type) {
        if (type == TransactionType.TRANSFER) {
            return null;
        }
        if (categoryId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Giao dịch thu/chi bắt buộc chọn danh mục.");
        }
        Category category = categories.findById(categoryId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy danh mục."));
        if (!category.getFamilyId().equals(familyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy danh mục.");
        }
        CategoryKind expected = type == TransactionType.INCOME ? CategoryKind.INCOME : CategoryKind.EXPENSE;
        if (category.getKind() != expected) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TX_CATEGORY_MISMATCH",
                "Danh mục '" + category.getName() + "' thuộc loại " + category.getKind()
                    + ", không dùng cho giao dịch " + type + ".");
        }
        return category;
    }

    /** BR-07: chuyển khoản là một dòng duy nhất, ví gửi khác ví nhận. */
    private Wallet resolveTransferTarget(Long familyId, TransactionType type, Long transferWalletId, Wallet source) {
        if (type != TransactionType.TRANSFER) {
            if (transferWalletId != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "TX_TRANSFER_NOT_ALLOWED",
                    "Chỉ giao dịch TRANSFER mới có ví nhận.");
            }
            return null;
        }
        if (transferWalletId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TX_TRANSFER_TARGET_REQUIRED",
                "Chuyển khoản cần chọn ví nhận.");
        }
        if (transferWalletId.equals(source.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TX_TRANSFER_SAME_WALLET",
                "Ví gửi và ví nhận không được trùng nhau.");
        }
        return requireWallet(familyId, transferWalletId);
    }

    private TransactionType parseType(String value) {
        try {
            return TransactionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "type chỉ nhận INCOME, EXPENSE hoặc TRANSFER.");
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
