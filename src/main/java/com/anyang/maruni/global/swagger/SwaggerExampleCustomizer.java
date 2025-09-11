package com.anyang.maruni.global.swagger;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.anyang.maruni.global.advice.ParameterData;
import com.anyang.maruni.global.response.dto.CommonApiResponse;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.anyang.maruni.global.response.success.SuccessCode;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Component
public class SwaggerExampleCustomizer {

	public void addErrorExamples(ApiResponses responses, SwaggerResponseDescription type) {
		Set<ErrorCode> errorCodeList = type.getErrorCodeList();

		Map<Integer, List<ExampleHolder>> statusWithExampleHolders =
			errorCodeList.stream()
				.map(
					errorCode ->
						ExampleHolder.builder()
							.holder(getSwaggerExample(errorCode))
							.code(errorCode.getHttpCode())
							.name(errorCode.toString())
							.build()
				)
				.collect(groupingBy(ExampleHolder::getCode));
		addExamplesToResponses(responses, statusWithExampleHolders);
	}

	public void addSuccessExample(ApiResponses responses, SuccessCode successCode) {
		Example successExample = getSuccessSwaggerExample(successCode);
		ExampleHolder exampleHolder = ExampleHolder.builder()
			.holder(successExample)
			.code(200)  // 성공 응답은 200으로 통일
			.name(successCode.toString())
			.build();

		Content content = new Content();
		MediaType mediaType = new MediaType();
		ApiResponse apiResponse = new ApiResponse();
		
		mediaType.addExamples(exampleHolder.getName(), exampleHolder.getHolder());
		content.addMediaType("application/json", mediaType);
		apiResponse.setDescription("성공 응답");
		apiResponse.setContent(content);
		
		responses.addApiResponse("200", apiResponse);
	}

	private Example getSwaggerExample(ErrorCode errorCode) {
		Object details = getErrorDetails(errorCode);
		return createExample(errorCode, errorCode.getMessage(), details);
	}

	private Example getSuccessSwaggerExample(SuccessCode successCode) {
		return createExample(successCode, successCode.getMessage(), null);
	}

	private Object getErrorDetails(ErrorCode errorCode) {
		if (errorCode == ErrorCode.PARAMETER_VALIDATION_ERROR) {
			return List.of(new ParameterData("field", "invalid-value", "검증 실패 예시"));
		}
		return null;
	}

	private <T> Example createExample(T type, String description, Object details) {
		CommonApiResponse<?> response;
		if (type instanceof ErrorCode) {
			response = details != null 
				? CommonApiResponse.failWithDetails((ErrorCode) type, details)
				: CommonApiResponse.fail((ErrorCode) type);
		} else {
			response = CommonApiResponse.success((SuccessCode) type);
		}
		
		Example example = new Example();
		example.description(description);
		example.setValue(response);
		return example;
	}

	private void addExamplesToResponses(ApiResponses responses,
		Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {
		statusWithExampleHolders.forEach(
			(status, v) -> {
				Content content = new Content();
				MediaType mediaType = new MediaType();
				ApiResponse apiResponse = new ApiResponse();
				v.forEach(
					exampleHolder ->
						mediaType.addExamples(exampleHolder.getName(), exampleHolder.getHolder()));
				content.addMediaType("application/json", mediaType);
				apiResponse.setDescription("에러 응답");
				apiResponse.setContent(content);
				responses.addApiResponse(status.toString(), apiResponse);
			});
	}
}