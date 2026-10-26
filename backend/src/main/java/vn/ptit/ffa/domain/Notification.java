package vn.ptit.ffa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Bảng `notifications` (V1) — dùng cho cảnh báo ngân sách 80%/vượt hạn mức (UC-08, BE-08). */
@Getter
@Setter
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_nt_user", columnList = "user_id, read_at"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "type", nullable = false, length = 40)
    private String type;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "body", length = 500)
    private String body;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Notification budgetAlert(Long userId, Long familyId, String title, String body) {
        Notification n = new Notification();
        n.userId = userId;
        n.familyId = familyId;
        n.type = "BUDGET_ALERT";
        n.title = title;
        n.body = body;
        return n;
    }

    public boolean isUnread() {
        return readAt == null;
    }
}
