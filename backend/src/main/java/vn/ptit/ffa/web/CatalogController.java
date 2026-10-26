package vn.ptit.ffa.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ptit.ffa.security.CurrentUser;
import vn.ptit.ffa.service.CatalogService;
import vn.ptit.ffa.web.dto.CatalogDto;

import java.util.List;

/** Danh muc va vi theo tung ho (DUAN-48, DUAN-49). */
@RestController
@RequestMapping("/api/v1/families/{fid}")
public class CatalogController {

    private final CatalogService catalog;
    private final CurrentUser currentUser;

    public CatalogController(CatalogService catalog, CurrentUser currentUser) {
        this.catalog = catalog;
        this.currentUser = currentUser;
    }

    @GetMapping("/categories")
    public List<CatalogDto.CategoryView> listCategories(@PathVariable Long fid,
                                                        @RequestParam(required = false) String kind) {
        return catalog.listCategories(currentUser.requireFamily(fid), kind);
    }

    @PostMapping("/categories")
    public ResponseEntity<CatalogDto.CategoryView> createCategory(@PathVariable Long fid,
                                                                  @Valid @RequestBody CatalogDto.CategoryRequest request) {
        currentUser.requireManager();
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.create(currentUser.requireFamily(fid), request));
    }

    @PutMapping("/categories/{id}")
    public CatalogDto.CategoryView updateCategory(@PathVariable Long fid, @PathVariable Long id,
                                                  @Valid @RequestBody CatalogDto.CategoryRequest request) {
        currentUser.requireManager();
        return catalog.update(currentUser.requireFamily(fid), id, request);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long fid, @PathVariable Long id) {
        currentUser.requireManager();
        catalog.delete(currentUser.requireFamily(fid), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/wallets")
    public List<CatalogDto.WalletView> listWallets(@PathVariable Long fid,
                                                   @RequestParam(defaultValue = "false") boolean includeArchived) {
        return catalog.listWallets(currentUser.requireFamily(fid), includeArchived);
    }

    @PostMapping("/wallets")
    public ResponseEntity<CatalogDto.WalletView> createWallet(@PathVariable Long fid,
                                                              @Valid @RequestBody CatalogDto.WalletRequest request) {
        currentUser.requireManager();
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.create(currentUser.requireFamily(fid), request));
    }

    @PatchMapping("/wallets/{id}")
    public CatalogDto.WalletView updateWallet(@PathVariable Long fid, @PathVariable Long id,
                                              @RequestBody CatalogDto.WalletUpdate request) {
        currentUser.requireManager();
        return catalog.update(currentUser.requireFamily(fid), id, request);
    }

    @DeleteMapping("/wallets/{id}")
    public ResponseEntity<Void> deleteWallet(@PathVariable Long fid, @PathVariable Long id) {
        currentUser.requireManager();
        catalog.deleteWallet(currentUser.requireFamily(fid), id);
        return ResponseEntity.noContent().build();
    }
}
