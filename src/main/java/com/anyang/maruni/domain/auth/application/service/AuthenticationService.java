package com.anyang.maruni.domain.auth.application.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.auth.domain.service.TokenManager;
import com.anyang.maruni.domain.auth.domain.vo.MemberTokenInfo;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.global.response.dto.CommonApiResponse;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.security.AuthenticationEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService implements AuthenticationEventHandler {

	private final TokenManager tokenManager;
	private final ObjectMapper objectMapper;
	private final MemberRepository memberRepository;

	@Override
	public void handleLoginSuccess(HttpServletResponse response, MemberTokenInfo memberInfo) {
		try {
			// Access Token 생성
			String accessToken = tokenManager.createAccessToken(
				memberInfo.memberId(),
				memberInfo.email()
			);

			// Member 정보 조회하여 역할 정보 추가
			Long memberId = Long.parseLong(memberInfo.memberId());
			MemberEntity member = memberRepository.findById(memberId)
				.orElseThrow(() -> new RuntimeException("Member not found"));

			Map<String, Object> data = new HashMap<>();
			data.put("accessToken", accessToken);
			data.put("tokenType", "Bearer");
			data.put("expiresIn", 3600);  // 1시간

			// Member 정보 (역할 포함)
			Map<String, Object> memberData = new HashMap<>();
			memberData.put("memberId", member.getId());
			memberData.put("memberEmail", member.getMemberEmail());
			memberData.put("memberName", member.getMemberName());
			memberData.put("dailyCheckEnabled", member.getDailyCheckEnabled());
			memberData.put("hasGuardian", member.hasGuardian());
			memberData.put("managedMembersCount", member.getManagedMembersCount());

			data.put("member", memberData);

			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json; charset=UTF-8");

			String json = objectMapper.writeValueAsString(
				CommonApiResponse.success(SuccessCode.MEMBER_LOGIN_SUCCESS, data)
			);
			response.getWriter().write(json);

			log.info("✅ 로그인 성공 - Member: {}, hasGuardian: {}, managedMembersCount: {}",
				member.getMemberEmail(), member.hasGuardian(), member.getManagedMembersCount());

		} catch (IOException e) {
			log.error("로그인 응답 처리 중 오류 발생: {}", e.getMessage());
			throw new RuntimeException("로그인 응답 처리 실패", e);
		}
	}
}
