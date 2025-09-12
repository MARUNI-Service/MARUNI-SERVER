package com.anyang.maruni.global.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.anyang.maruni.domain.auth.domain.service.TokenService;
import com.anyang.maruni.domain.member.infrastructure.security.CustomUserDetails;
import com.anyang.maruni.domain.member.application.dto.request.MemberLoginRequest;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginFilter 단위 테스트")
class LoginFilterTest {

	@Mock
	private AuthenticationManager authenticationManager;
	
	@Mock
	private TokenService tokenService;
	
	@Mock
	private AuthenticationEventHandler eventHandler;
	
	@Mock
	private ObjectMapper objectMapper;

	private LoginFilter loginFilter;
	
	@BeforeEach
	void setUp() {
		loginFilter = new LoginFilter(authenticationManager, objectMapper, eventHandler);
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@Test
	@DisplayName("로그인 인증 시도 - 정상적인 JSON 요청")
	void attemptAuthentication_Success_ValidJson() throws Exception {
		// given
		String loginJson = """
			{
				"memberEmail": "test@example.com",
				"memberPassword": "password123"
			}
			""";
		
		request.setContent(loginJson.getBytes());
		request.setContentType("application/json");
		request.setMethod("POST");

		MemberLoginRequest loginRequest = new MemberLoginRequest();
		loginRequest.setMemberEmail("test@example.com");
		loginRequest.setMemberPassword("password123");
		
		given(objectMapper.readValue(any(InputStream.class), eq(MemberLoginRequest.class)))
			.willReturn(loginRequest);

		Authentication mockAuth = new UsernamePasswordAuthenticationToken("test@example.com", "password123");
		given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.willReturn(mockAuth);

		// when
		Authentication result = loginFilter.attemptAuthentication(request, response);

		// then
		assertThat(result).isEqualTo(mockAuth);
		then(objectMapper).should().readValue(any(InputStream.class), eq(MemberLoginRequest.class));
		then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
	}

	@Test
	@DisplayName("로그인 인증 시도 실패 - 잘못된 JSON 형식")
	void attemptAuthentication_Fail_InvalidJson() throws Exception {
		// given
		String invalidJson = "{ invalid json }";
		request.setContent(invalidJson.getBytes());
		request.setContentType("application/json");
		request.setMethod("POST");

		given(objectMapper.readValue(any(InputStream.class), eq(MemberLoginRequest.class)))
			.willThrow(new RuntimeException("Invalid JSON"));

		// when & then
		assertThatThrownBy(() -> loginFilter.attemptAuthentication(request, response))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Invalid JSON");
	}

	@Test
	@DisplayName("로그인 성공 처리 - JWT 토큰 생성 및 쿠키 설정")
	void successfulAuthentication_Success() throws IOException {
		// given
		MemberEntity member = mock(MemberEntity.class);
		given(member.getId()).willReturn(1L);
		
		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		given(userDetails.getUsername()).willReturn("test@example.com");
		given(userDetails.getMember()).willReturn(member);
		
		Authentication authentication = mock(Authentication.class);
		given(authentication.getPrincipal()).willReturn(userDetails);

		// AuthenticationEventHandler mock 설정
		willDoNothing().given(eventHandler).handleLoginSuccess(any(), any());

		// when
		loginFilter.successfulAuthentication(request, response, null, authentication);

		// then
		then(eventHandler).should().handleLoginSuccess(any(), any());
	}

	@Test
	@DisplayName("로그인 실패 처리 - 에러 응답")
	void unsuccessfulAuthentication_Fail() throws IOException {
		// given
		AuthenticationException exception = mock(AuthenticationException.class);

		// ObjectMapper mock 설정 - 호출 전에 설정해야 함
		String expectedJson = "{\"code\":\"E401\",\"message\":\"로그인 실패\"}";
		given(objectMapper.writeValueAsString(any()))
			.willReturn(expectedJson);

		// when
		loginFilter.unsuccessfulAuthentication(request, response, exception);

		// then
		then(objectMapper).should().writeValueAsString(any());
		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).isEqualTo("application/json; charset=UTF-8");
	}
}