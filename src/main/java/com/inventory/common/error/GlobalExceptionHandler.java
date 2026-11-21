package com.inventory.common.error;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(final BusinessException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
			.body(ErrorResponse.of(errorCode, ex.getMessage()));
	}

	@ExceptionHandler(PessimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleLockFailure(final PessimisticLockingFailureException ex) {
		log.warn("Lock acquisition failed", ex);
		return ResponseEntity.status(ErrorCode.LOCK_TIMEOUT.getStatus())
			.body(ErrorResponse.of(ErrorCode.LOCK_TIMEOUT));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(final Exception ex) {
		log.error("Unexpected error", ex);
		return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
			.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
		final MethodArgumentNotValidException ex,
		final HttpHeaders headers,
		final HttpStatusCode status,
		final WebRequest request) {
		List<ErrorResponse.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new ErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage()))
			.toList();
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, violations));
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(
		final HandlerMethodValidationException ex,
		final HttpHeaders headers,
		final HttpStatusCode status,
		final WebRequest request) {
		if (status.is5xxServerError()) {
			return handleExceptionInternal(ex, null, headers, status, request);
		}
		List<ErrorResponse.FieldViolation> violations = ex.getParameterValidationResults().stream()
			.flatMap(this::toViolations)
			.toList();
		return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
			.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, violations));
	}

	private Stream<ErrorResponse.FieldViolation> toViolations(final ParameterValidationResult result) {
		if (result instanceof ParameterErrors errors) {
			return errors.getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage()));
		}
		String parameterName = result.getMethodParameter().getParameterName();
		return result.getResolvableErrors().stream()
			.map(error -> new ErrorResponse.FieldViolation(parameterName, error.getDefaultMessage()));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
		final Exception ex,
		final Object body,
		final HttpHeaders headers,
		final HttpStatusCode statusCode,
		final WebRequest request) {
		if (statusCode.is5xxServerError()) {
			log.error("Request failed", ex);
			return ResponseEntity.status(statusCode).headers(headers)
				.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
		}
		if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
			return ResponseEntity.status(statusCode).headers(headers)
				.body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
		}
		HttpStatus status = HttpStatus.valueOf(statusCode.value());
		return ResponseEntity.status(statusCode).headers(headers)
			.body(new ErrorResponse(status.name(), status.getReasonPhrase(), List.of()));
	}
}
