package vn.ptit.ffa.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ptit.ffa.security.CurrentUser;
import vn.ptit.ffa.service.BudgetService;
import vn.ptit.ffa.web.dto.BudgetDto;

import java.time.LocalDate;
import java.util.List;

/** Ngân sách theo danh mục và kỳ (DUAN-53, DUAN-55). */
@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgets;
    private final CurrentUser currentUser;

    public BudgetController(BudgetService budgets, CurrentUser currentUser) {
        this.budgets = budgets;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<BudgetDto.View> list(@RequestParam(required = false) LocalDate periodStart,
                                     @RequestParam(required = false) String status) {
        var all = budgets.list(currentUser.require(), periodStart);
        if (status == null || status.isBlank()) {
            return all;
        }
        return all.stream().filter(view -> status.equalsIgnoreCase(view.status())).toList();
    }

    @PutMapping
    public BudgetDto.View upsert(@Valid @RequestBody BudgetDto.Request request) {
        currentUser.requireWriter();
        return budgets.upsert(currentUser.require(), request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetDto.View create(@Valid @RequestBody BudgetDto.Request request) {
        currentUser.requireWriter();
        return budgets.upsert(currentUser.require(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        currentUser.requireManager();
        budgets.delete(currentUser.require(), id);
        return ResponseEntity.noContent().build();
    }
}
