package com.anyang.maruni.global.response.success;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements SuccessType {
	// Common Success Codes
	SUCCESS("S200", "성공"),
	
	// Member Success Codes (M2xx)
	MEMBER_LOGIN_SUCCESS("M201", "로그인 성공"),
	MEMBER_LOGOUT_SUCCESS("M202", "로그아웃 성공"),
	MEMBER_TOKEN_REISSUE_SUCCESS("M203", "Access 토큰 재발급 성공"),
	MEMBER_TOKEN_REISSUE_FULL_SUCCESS("M204", "Access/Refresh 토큰 재발급 성공"),
	MEMBER_EMAIL_CHECK_OK("M205", "이메일 사용 가능"),
	MEMBER_CREATED("M206", "회원가입 성공"),
	MEMBER_UPDATED("M207", "회원 정보 수정 성공"),
	MEMBER_DELETED("M208", "회원 삭제 성공"),
	MEMBER_VIEW("M209", "회원 정보 조회 성공"),
	MEMBER_SOCIAL_LOGIN_SUCCESS("M210", "소셜 로그인 성공");

	private final String code;
	private final String message;
}