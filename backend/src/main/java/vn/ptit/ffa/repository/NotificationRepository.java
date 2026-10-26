package vn.ptit.ffa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.ptit.ffa.domain.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select count(n) from Notification n where n.userId = :userId and n.readAt is null")
    long countUnread(@Param("userId") Long userId);

    List<Notification> findByFamilyIdAndTypeOrderByCreatedAtDesc(Long familyId, String type);
}
