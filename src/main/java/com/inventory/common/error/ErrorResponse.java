package com.inventory.common.error;

import java.util.List;

public record ErrorResponse(String code, String message, List<FieldViolation> errors) {

	public record FieldViolation(String field, String reason) {
	}

	public static ErrorResponse of(final ErrorCode errorCode) {
		return new ErrorResponse(errorCode.name(), errorCode.getDefaultMessage(), List.of());
	}

	public static ErrorResponse of(final ErrorCode errorCode, final String message) {
		return new ErrorResponse(errorCode.name(), message, List.of());
	}

	public static ErrorResponse of(final ErrorCode errorCode, final List<FieldViolation> errors) {
		return new ErrorResponse(errorCode.name(), errorCode.getDefaultMessage(), errors);
	}
}
