package com.anyang.maruni.domain.member.presentation.controller;

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

import com.anyang.maruni.domain.member.application.service.MemberService;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("JoinApiController 통합 테스트")
class JoinApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MemberService memberService;

	@Test
	@DisplayName("회원가입 성공 - 정상적인 요청")
	@WithMockUser
	void join_Success() throws Exception {
		// given
		String requestBody = """
			{
				"memberEmail": "test@example.com",
				"memberName": "테스트 사용자",
				"memberPassword": "password123!"
			}
			""";

		willDoNothing().given(memberService).save(any());

		// when & then
		mockMvc.perform(post("/api/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("M206"))
			.andExpect(jsonPath("$.message").exists());

		then(memberService).should().save(any());
	}

	@Test
	@DisplayName("회원가입 실패 - 이메일 누락")
	@WithMockUser
	void join_Fail_MissingEmail() throws Exception {
		// given
		String requestBody = """
			{
				"memberName": "테스트 사용자",
				"memberPassword": "password123!"
			}
			""";

		// when & then
		mockMvc.perform(post("/api/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E400"));

		then(memberService).should(never()).save(any());
	}

	@Test
	@DisplayName("회원가입 실패 - 잘못된 이메일 형식")
	@WithMockUser
	void join_Fail_InvalidEmailFormat() throws Exception {
		// given
		String requestBody = """
			{
				"memberEmail": "invalid-email-format",
				"memberName": "테스트 사용자",
				"memberPassword": "password123!"
			}
			""";

		// when & then
		mockMvc.perform(post("/api/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E400"));

		then(memberService).should(never()).save(any());
	}

	@Test
	@DisplayName("회원가입 실패 - 이름 누락")
	@WithMockUser
	void join_Fail_MissingName() throws Exception {
		// given
		String requestBody = """
			{
				"memberEmail": "test@example.com",
				"memberPassword": "password123!"
			}
			""";

		// when & then
		mockMvc.perform(post("/api/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E400"));

		then(memberService).should(never()).save(any());
	}

	@Test
	@DisplayName("회원가입 실패 - 비밀번호 누락")
	@WithMockUser
	void join_Fail_MissingPassword() throws Exception {
		// given
		String requestBody = """
			{
				"memberEmail": "test@example.com",
				"memberName": "테스트 사용자"
			}
			""";

		// when & then
		mockMvc.perform(post("/api/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E400"));

		then(memberService).should(never()).save(any());
	}

	@Test
	@DisplayName("회원가입 실패 - 이메일 중복")
	@WithMockUser
	void join_Fail_DuplicateEmail() throws Exception {
		// given
		String requestBody = """
			{
				"memberEmail": "duplicate@example.com",
				"memberName": "테스트 사용자",
				"memberPassword": "password123!"
			}
			""";

		willThrow(new BaseException(ErrorCode.DUPLICATE_EMAIL))
			.given(memberService).save(any());

		// when & then
		mockMvc.perform(post("/api/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E409"));

		then(memberService).should().save(any());
	}

	@Test
	@DisplayName("이메일 중복 확인 성공 - 사용 가능한 이메일")
	@WithMockUser
	void emailCheck_Success_Available() throws Exception {
		// given
		String availableEmail = "available@example.com";
		given(memberService.isEmailAvailable(availableEmail)).willReturn(true);

		// when & then
		mockMvc.perform(get("/api/join/email-check")
				.param("memberEmail", availableEmail))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("M205"));

		then(memberService).should().isEmailAvailable(availableEmail);
	}

	@Test
	@DisplayName("이메일 중복 확인 실패 - 이미 존재하는 이메일")
	@WithMockUser
	void emailCheck_Fail_Duplicate() throws Exception {
		// given
		String existingEmail = "existing@example.com";
		given(memberService.isEmailAvailable(existingEmail)).willReturn(false);

		// when & then
		mockMvc.perform(get("/api/join/email-check")
				.param("memberEmail", existingEmail))
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").exists())
			.andExpect(jsonPath("$.code").value("E409"));

		then(memberService).should().isEmailAvailable(existingEmail);
	}

	@Test
	@DisplayName("이메일 중복 확인 실패 - 이메일 파라미터 누락")
	@WithMockUser
	void emailCheck_Fail_MissingParameter() throws Exception {
		// when & then
		mockMvc.perform(get("/api/join/email-check"))
			.andDo(print())
			.andExpect(status().isBadRequest());

		then(memberService).should(never()).isEmailAvailable(anyString());
	}
}