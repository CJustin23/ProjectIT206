package vn.ptit.ffa.web.dto;

/** A single field-level validation detail (api.md `details[]`). */
public record FieldError(String field, String reason) {
}
