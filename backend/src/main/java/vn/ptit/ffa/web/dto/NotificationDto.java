package vn.ptit.ffa.web.dto;

import java.time.LocalDateTime;

/** Thông báo cảnh báo cho người dùng (api.md muc 2.6). */
public final class NotificationDto {

    private NotificationDto() {
    }

    public record View(Long id, String type, String title, String body, LocalDateTime readAt, LocalDateTime createdAt) {
    }

    public record CountView(long unread) {
    }
}
