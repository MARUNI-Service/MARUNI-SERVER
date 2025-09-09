package com.anyang.maruni.global.response.dto;

import com.anyang.maruni.global.response.error.ErrorType;
import com.anyang.maruni.global.response.success.SuccessType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "공통 API 응답")
public class CommonApiResponse<T> {

	@Schema(description = "응답 코드", example = "M001" /* 또는 E001 등 */)
	private final String code;

	@Schema(description = "응답 메시지", example = "회원 정보 조회 성공")
	private final String message;

	@Schema(description = "응답 데이터", nullable = true)
	private final T data;

	// 성공 응답
	public static <T> CommonApiResponse<T> success(SuccessType successCode, T data) {
		return new CommonApiResponse<>(successCode.getCode(), successCode.getMessage(), data);
	}

	public static CommonApiResponse<Void> success(SuccessType successCode) {
		return new CommonApiResponse<>(successCode.getCode(), successCode.getMessage(), null);
	}

	// 실패 응답
	public static CommonApiResponse<Void> fail(ErrorType errorType) {
		return new CommonApiResponse<>(errorType.getCode(), errorType.getMessage(), null);
	}

	// 상세 정보가 있는 실패 응답
	public static <T> CommonApiResponse<T> failWithDetails(ErrorType errorType, T details) {
		return new CommonApiResponse<>(errorType.getCode(), errorType.getMessage(), details);
	}

	// 생성자
	private CommonApiResponse(String code, String message, T data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}
}
