package vn.ptit.ffa.error;

import org.springframework.http.HttpStatus;

/**
 * Application exception carrying the api.md standard error {@code code} and HTTP status.
 * Translated to the standard error body by {@link vn.ptit.ffa.web.GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
