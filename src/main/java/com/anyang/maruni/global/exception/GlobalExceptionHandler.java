package com.anyang.maruni.global.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.anyang.maruni.global.advice.ParameterData;
import com.anyang.maruni.global.response.dto.CommonApiResponse;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 1. 커스텀 예외 처리
	@ExceptionHandler(BaseException.class)
	public ResponseEntity<?> handleBaseException(BaseException e) {
		return ResponseEntity
			.status(e.getErrorCode().getStatus())
			.body(CommonApiResponse.fail(e.getErrorCode()));
	}

	// 2. Enum 변환 에러 (Role 등)
	@ExceptionHandler(InvalidFormatException.class)
	public ResponseEntity<?> handleInvalidFormat(InvalidFormatException e) {
		if (e.getTargetType().isEnum() && e.getTargetType().getSimpleName().equals("Role")) {
			return ResponseEntity
				.status(ErrorCode.INVALID_ROLE.getStatus())
				.body(CommonApiResponse.fail(ErrorCode.INVALID_ROLE));
		}

		return ResponseEntity
			.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
			.body(CommonApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
	}

	// 3. Bean Validation 실패 예외
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException e) {
		List<ParameterData> errors = e.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new ParameterData(
				error.getField(),
				error.getRejectedValue() != null ? error.getRejectedValue().toString() : "null",
				error.getDefaultMessage()
			))
			.collect(Collectors.toList());

		log.warn("Validation failed: {}", errors);

		return ResponseEntity
			.status(ErrorCode.PARAMETER_VALIDATION_ERROR.getStatus())
			.body(CommonApiResponse.failWithDetails(ErrorCode.PARAMETER_VALIDATION_ERROR, errors));
	}
}