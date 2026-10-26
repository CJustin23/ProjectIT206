package vn.ptit.ffa.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ptit.ffa.domain.TransactionType;
import vn.ptit.ffa.security.CurrentUser;
import vn.ptit.ffa.service.TransactionService;
import vn.ptit.ffa.web.dto.TransactionDto;

import java.time.LocalDate;

/** CRUD giao dịch cua ho dang dang nhap (DUAN-50). */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService service;
    private final CurrentUser currentUser;

    public TransactionController(TransactionService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public TransactionDto.PageView<TransactionDto.View> list(
        @RequestParam(required = false) TransactionType type,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long walletId,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return service.search(currentUser.require(), type, categoryId, walletId, from, to, keyword, page, size);
    }

    @GetMapping("/{id}")
    public TransactionDto.View get(@PathVariable Long id) {
        return service.get(currentUser.require(), id);
    }

    @PostMapping
    public ResponseEntity<TransactionDto.View> create(@Valid @RequestBody TransactionDto.CreateRequest request) {
        currentUser.requireWriter();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(currentUser.require(), request));
    }

    @PutMapping("/{id}")
    public TransactionDto.View update(@PathVariable Long id, @Valid @RequestBody TransactionDto.UpdateRequest request) {
        currentUser.requireWriter();
        return service.update(currentUser.require(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        currentUser.requireWriter();
        service.delete(currentUser.require(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to) {
        String csv = service.exportCsv(currentUser.require(), from, to);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
            .body(csv);
    }
}
