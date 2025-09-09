package com.anyang.maruni.global.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.anyang.maruni.global.response.success.SuccessCode;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SuccessResponseDescription {
	SuccessCode value() default SuccessCode.SUCCESS;
}