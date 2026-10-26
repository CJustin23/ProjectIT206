package vn.ptit.ffa.web.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Standard error body shared by the exception handler and the security entry points (api.md section 1).
 * The FE switches on {@code code}; {@code message} is display text only.
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        List<FieldError> details) {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(OffsetDateTime.now(BUSINESS_ZONE), status, code, message, List.of());
    }

    public static ErrorResponse of(int status, String code, String message, List<FieldError> details) {
        return new ErrorResponse(OffsetDateTime.now(BUSINESS_ZONE), status, code, message, details);
    }
}
