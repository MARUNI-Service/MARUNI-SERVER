package com.anyang.maruni.global.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.anyang.maruni.global.response.annotation.AutoApiResponse;
import com.anyang.maruni.global.response.annotation.SuccessCodeAnnotation;
import com.anyang.maruni.global.response.dto.CommonApiResponse;
import com.anyang.maruni.global.response.success.SuccessCode;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return returnType.hasMethodAnnotation(AutoApiResponse.class)
			|| returnType.getDeclaringClass().isAnnotationPresent(AutoApiResponse.class);
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
		Class<? extends HttpMessageConverter<?>> selectedConverterType,
		ServerHttpRequest request, ServerHttpResponse response) {

		if (body instanceof CommonApiResponse) {
			return body;
		}

		// @SuccessCodeAnnotation 어노테이션이 있으면 지정된 성공 코드 사용
		SuccessCodeAnnotation successCodeAnnotation = returnType.getMethodAnnotation(SuccessCodeAnnotation.class);
		SuccessCode successCode = successCodeAnnotation != null
			? successCodeAnnotation.value()
			: SuccessCode.SUCCESS;

		if (body == null || returnType.getParameterType() == Void.class) {
			return CommonApiResponse.success(successCode);
		}

		return CommonApiResponse.success(successCode, body);
	}
}
