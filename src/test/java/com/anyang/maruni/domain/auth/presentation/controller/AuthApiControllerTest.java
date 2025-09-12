package com.anyang.maruni.domain.auth.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.anyang.maruni.domain.auth.application.dto.response.TokenResponse;
import com.anyang.maruni.domain.auth.application.service.AuthenticationService;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthApiController 통합 테스트")
class AuthApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticationService authenticationService;

	@Test
	@DisplayName("Access Token 재발급 성공")
	@WithMockUser
	void refreshAccessToken_Success() throws Exception {
		// given
		TokenResponse tokenResponse = TokenResponse.accessOnly("new-access-token", 3600L);
		
		given(authenticationService.refreshAccessToken(any(), any()))
			.willReturn(tokenResponse);

		// when & then
		mockMvc.perform(post("/api/auth/token/refresh")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("M203"))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"));

		then(authenticationService).should().refreshAccessToken(any(), any());
	}

	@Test
	@DisplayName("Access Token 재발급 실패 - 유효하지 않은 토큰")
	@WithMockUser
	void refreshAccessToken_Fail_InvalidToken() throws Exception {
		// given
		willThrow(new BaseException(ErrorCode.INVALID_TOKEN))
			.given(authenticationService).refreshAccessToken(any(), any());

		// when & then
		mockMvc.perform(post("/api/auth/token/refresh")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E401"));

		then(authenticationService).should().refreshAccessToken(any(), any());
	}

	@Test
	@DisplayName("Access + Refresh Token 모두 재발급 성공")
	@WithMockUser
	void refreshAllTokens_Success() throws Exception {
		// given
		TokenResponse tokenResponse = TokenResponse.withRefresh("new-access-token", 3600L);
		
		given(authenticationService.refreshAllTokens(any(), any()))
			.willReturn(tokenResponse);

		// when & then
		mockMvc.perform(post("/api/auth/token/refresh/full")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("M204"))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.data.refreshTokenIncluded").value(true));

		then(authenticationService).should().refreshAllTokens(any(), any());
	}

	@Test
	@DisplayName("전체 토큰 재발급 실패 - 유효하지 않은 토큰")
	@WithMockUser
	void refreshAllTokens_Fail_InvalidToken() throws Exception {
		// given
		willThrow(new BaseException(ErrorCode.INVALID_TOKEN))
			.given(authenticationService).refreshAllTokens(any(), any());

		// when & then
		mockMvc.perform(post("/api/auth/token/refresh/full")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E401"));

		then(authenticationService).should().refreshAllTokens(any(), any());
	}

	@Test
	@DisplayName("로그아웃 성공")
	@WithMockUser
	void logout_Success() throws Exception {
		// given
		willDoNothing().given(authenticationService).logout(any(), any());

		// when & then
		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("M202"));

		then(authenticationService).should().logout(any(), any());
	}

	@Test
	@DisplayName("로그아웃 실패 - 유효하지 않은 토큰")
	@WithMockUser
	void logout_Fail_InvalidToken() throws Exception {
		// given
		willThrow(new BaseException(ErrorCode.INVALID_TOKEN))
			.given(authenticationService).logout(any(), any());

		// when & then
		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E401"));

		then(authenticationService).should().logout(any(), any());
	}
}