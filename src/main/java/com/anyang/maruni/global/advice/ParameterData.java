package com.anyang.maruni.global.advice;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParameterData {
	private String key;
	private String value;
	private String reason;
}
