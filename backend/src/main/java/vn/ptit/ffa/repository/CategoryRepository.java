package vn.ptit.ffa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.ffa.domain.Category;
import vn.ptit.ffa.domain.CategoryKind;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByFamilyIdOrderBySortOrderAscIdAsc(Long familyId);

    List<Category> findByFamilyIdAndKindOrderBySortOrderAscIdAsc(Long familyId, CategoryKind kind);

    Optional<Category> findByFamilyIdAndKindAndName(Long familyId, CategoryKind kind, String name);

    boolean existsByParentId(Long parentId);
}
