package com.anyang.maruni.domain.member.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

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

import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.service.MemberService;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserApiController 통합 테스트")
class UserApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberService memberService;

	@Test
	@DisplayName("전체 회원 조회 성공")
	@WithMockUser(roles = "USER")
	void findAll_Success_WithAdminRole() throws Exception {
		// given
		List<MemberResponse> responses = List.of(
			MemberResponse.builder().id(1L).memberEmail("user1@example.com").memberName("사용자1").build(),
			MemberResponse.builder().id(2L).memberEmail("user2@example.com").memberName("사용자2").build()
		);

		given(memberService.findAll()).willReturn(responses);

		// when & then
		mockMvc.perform(get("/api/users")
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("M209"))
			.andExpect(jsonPath("$.message").value("회원 정보 조회 성공"))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].id").value(1))
			.andExpect(jsonPath("$.data[0].memberEmail").value("user1@example.com"))
			.andExpect(jsonPath("$.data[1].id").value(2))
			.andExpect(jsonPath("$.data[1].memberEmail").value("user2@example.com"));

		then(memberService).should().findAll();
	}


	@Test
	@DisplayName("특정 회원 조회 성공 - USER 권한")
	@WithMockUser(roles = "USER")
	void findById_Success_WithUserRole() throws Exception {
		// given
		Long memberId = 1L;
		MemberResponse response = MemberResponse.builder()
			.id(memberId)
			.memberEmail("test@example.com")
			.memberName("테스트 사용자")
			.build();

		given(memberService.findById(memberId)).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/users/{id}", memberId)
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("M209"))
			.andExpect(jsonPath("$.message").value("회원 정보 조회 성공"))
			.andExpect(jsonPath("$.data.id").value(memberId))
			.andExpect(jsonPath("$.data.memberEmail").value("test@example.com"))
			.andExpect(jsonPath("$.data.memberName").value("테스트 사용자"));

		then(memberService).should().findById(memberId);
	}

	@Test
	@DisplayName("특정 회원 조회 실패 - 존재하지 않는 회원")
	@WithMockUser(roles = "USER")
	void findById_Fail_MemberNotFound() throws Exception {
		// given
		Long memberId = 999L;
		given(memberService.findById(memberId))
			.willThrow(new BaseException(ErrorCode.MEMBER_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/api/users/{id}", memberId)
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("E404"))
			.andExpect(jsonPath("$.message").exists());

		then(memberService).should().findById(memberId);
	}

	@Test
	@DisplayName("회원 정보 수정 성공 - USER 권한")
	@WithMockUser(roles = "USER")
	void update_Success_WithUserRole() throws Exception {
		// given
		Long memberId = 1L;
		String requestBody = """
			{
				"memberName": "수정된 이름",
				"memberPassword": "newPassword123!"
			}
			""";

		willDoNothing().given(memberService).update(any());

		// when & then
		mockMvc.perform(put("/api/users/{id}", memberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("M207"))
			.andExpect(jsonPath("$.message").value("회원 정보 수정 성공"));

		then(memberService).should().update(any());
	}

	@Test
	@DisplayName("회원 정보 수정 실패 - 존재하지 않는 회원")
	@WithMockUser(roles = "USER")
	void update_Fail_MemberNotFound() throws Exception {
		// given
		Long memberId = 999L;
		String requestBody = """
			{
				"memberName": "수정된 이름",
				"memberPassword": "newPassword123!"
			}
			""";

		willThrow(new BaseException(ErrorCode.MEMBER_NOT_FOUND))
			.given(memberService).update(any());

		// when & then
		mockMvc.perform(put("/api/users/{id}", memberId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("E404"))
			.andExpect(jsonPath("$.message").exists());

		then(memberService).should().update(any());
	}

	@Test
	@DisplayName("회원 삭제 성공")
	@WithMockUser(roles = "USER")
	void delete_Success_WithAdminRole() throws Exception {
		// given
		Long memberId = 1L;
		willDoNothing().given(memberService).deleteById(memberId);

		// when & then
		mockMvc.perform(delete("/api/users/{id}", memberId)
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("M208"))
			.andExpect(jsonPath("$.message").value("회원 삭제 성공"));

		then(memberService).should().deleteById(memberId);
	}


	@Test
	@DisplayName("회원 삭제 실패 - 존재하지 않는 회원")
	@WithMockUser(roles = "USER")
	void delete_Fail_MemberNotFound() throws Exception {
		// given
		Long memberId = 999L;
		willThrow(new BaseException(ErrorCode.MEMBER_NOT_FOUND))
			.given(memberService).deleteById(memberId);

		// when & then
		mockMvc.perform(delete("/api/users/{id}", memberId)
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("E404"))
			.andExpect(jsonPath("$.message").exists());

		then(memberService).should().deleteById(memberId);
	}

	@Test
	@DisplayName("인증되지 않은 사용자의 접근 실패")
	void access_Fail_Unauthenticated() throws Exception {
		// when & then
		mockMvc.perform(get("/api/users"))
			.andDo(print())
			.andExpect(status().isUnauthorized());

		then(memberService).should(never()).findAll();
	}

	@Test
	@DisplayName("잘못된 경로 파라미터 - 숫자가 아닌 ID")
	@WithMockUser(roles = "USER")
	void findById_Fail_InvalidPathVariable() throws Exception {
		// when & then
		mockMvc.perform(get("/api/users/{id}", "invalid-id")
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isBadRequest());

		then(memberService).should(never()).findById(anyLong());
	}
}