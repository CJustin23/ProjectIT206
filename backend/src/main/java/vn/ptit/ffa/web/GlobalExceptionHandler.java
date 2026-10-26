package vn.ptit.ffa.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.ptit.ffa.error.ApiException;
import vn.ptit.ffa.web.dto.ErrorResponse;
import vn.ptit.ffa.web.dto.FieldError;

import java.util.List;
import java.util.NoSuchElementException;

/** Dich exception sang cau truc loi chuan trong docs/api.md muc 1. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus().value())
            .body(ErrorResponse.of(e.getStatus().value(), e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> details = e.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(400, "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ.", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(400, "VALIDATION_FAILED", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(400, "VALIDATION_FAILED", "Body JSON không đọc được."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.of(403, "FORBIDDEN_ROLE", "Bạn không có quyền thực hiện thao tác này."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.of(401, "AUTH_INVALID_CREDENTIALS", "Chưa xác thực hoặc phiên không còn hiệu lực."));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(404, "NOT_FOUND", "Không tìm thấy bản ghi."));
    }

    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> handleDuplicate(Exception e) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(409, "CONFLICT", "Dữ liệu bị trùng hoặc vi phạm ràng buộc."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e) {
        log.error("Loi chưa được xử lý", e);
        return ResponseEntity.internalServerError()
            .body(ErrorResponse.of(500, "INTERNAL_ERROR", "Có lỗi từ hệ thống, vui lòng thử lại."));
    }
}
