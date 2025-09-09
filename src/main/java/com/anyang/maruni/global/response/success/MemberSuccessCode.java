package com.anyang.maruni.global.response.success;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements SuccessType {
	SUCCESS("S200", "성공"),
	LOGIN_SUCCESS("S201", "로그인 성공"),
	LOGOUT_SUCCESS("S201", "로그아웃 성공"),
	TOKEN_REISSUE_SUCCESS("S202", "Access 토큰 재발급 성공"),
	TOKEN_REISSUE_FULL_SUCCESS("S203", "Access/Refresh 토큰 재발급 성공"),
	EMAIL_CHECK_OK("S204", "이메일 사용 가능"),
	MEMBER_CREATED("S205", "회원가입 성공"),
	MEMBER_UPDATED("S206", "회원 정보 수정 성공"),
	MEMBER_DELETED("S207", "회원 삭제 성공"),
	MEMBER_VIEW("S208", "회원 정보 조회 성공"),
	SOCIAL_LOGIN_SUCCESS("S209", "소셜 로그인 성공");

	private final String code;
	private final String message;
}
