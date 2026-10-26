package vn.ptit.ffa.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.repository.NotificationRepository;
import vn.ptit.ffa.security.CurrentUser;
import vn.ptit.ffa.web.dto.NotificationDto;

import java.time.LocalDateTime;
import java.util.List;

/** Danh sách cảnh báo ngân sách của người đang đăng nhập (DUAN-55). */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationRepository notifications;
    private final CurrentUser currentUser;

    public NotificationController(NotificationRepository notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NotificationDto.View> list(@RequestParam(defaultValue = "false") boolean unread) {
        var principal = currentUser.require();
        var rows = unread
            ? notifications.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(principal.userId())
            : notifications.findByUserIdOrderByCreatedAtDesc(principal.userId());
        return rows.stream()
            .map(row -> new NotificationDto.View(row.getId(), row.getType(), row.getTitle(), row.getBody(),
                row.getReadAt(), row.getCreatedAt()))
            .toList();
    }

    @GetMapping("/unread-count")
    public NotificationDto.CountView unreadCount() {
        return new NotificationDto.CountView(notifications.countUnread(currentUser.require().userId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        var notification = notifications.findById(id)
            .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "NOT_FOUND",
                "Không tìm thấy thông báo."));
        if (!notification.getUserId().equals(currentUser.require().userId())) {
            throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "NOT_FOUND",
                "Không tìm thấy thông báo.");
        }
        notification.setReadAt(LocalDateTime.now());
        notifications.save(notification);
        return ResponseEntity.noContent().build();
    }
}
