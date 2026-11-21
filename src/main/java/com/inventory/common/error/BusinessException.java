package com.inventory.common.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(final ErrorCode errorCode) {
		this(errorCode, errorCode.getDefaultMessage());
	}

	public BusinessException(final ErrorCode errorCode, final String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
