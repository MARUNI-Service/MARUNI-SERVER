package com.anyang.maruni.integration;

import com.anyang.maruni.domain.alertrule.application.service.core.AlertRuleService;
import com.anyang.maruni.domain.alertrule.domain.entity.AlertLevel;
import com.anyang.maruni.domain.alertrule.domain.repository.AlertHistoryRepository;
import com.anyang.maruni.domain.conversation.domain.entity.ConversationEntity;
import com.anyang.maruni.domain.conversation.domain.entity.EmotionType;
import com.anyang.maruni.domain.conversation.domain.entity.MessageEntity;
import com.anyang.maruni.domain.conversation.domain.repository.ConversationRepository;
import com.anyang.maruni.domain.guardian.application.dto.GuardianRequestDto;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianRelation;
import com.anyang.maruni.domain.guardian.domain.repository.GuardianRequestRepository;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.service.NotificationService;
import com.anyang.maruni.global.security.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MARUNI 사용자 여정 통합 테스트
 *
 * user-journey.md의 Journey 1-4 시나리오를 검증합니다.
 * - Journey 1: 첫 시작 (김순자 할머니 회원가입)
 * - Journey 2: 첫 안부 메시지 받기 (안부 메시지 자동 발송은 별도 테스트)
 * - Journey 3: 보호자 등록 (김순자 → 김영희에게 요청)
 * - Journey 4: 보호자의 알림 받기 (김영희가 요청 수락)
 */
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("사용자 여정 통합 테스트")
public class UserJourneyIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private GuardianRequestRepository guardianRequestRepository;

	@Autowired
	private ConversationRepository conversationRepository;

	@Autowired
	private AlertRuleService alertRuleService;

	@Autowired
	private AlertHistoryRepository alertHistoryRepository;

	@Autowired
	private JWTUtil jwtUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockBean
	private NotificationService notificationService;

	// 테스트 데이터
	private MemberEntity soonja;  // 김순자 할머니 (노인)
	private MemberEntity younghee;  // 김영희 (보호자/딸)
	private String soonjaToken;
	private String youngheeToken;

	@BeforeEach
	void setUp() {
		// 김순자 할머니 (노인 역할)
		soonja = MemberEntity.builder()
			.memberEmail("soonja@example.com")
			.memberName("김순자")
			.memberPassword(passwordEncoder.encode("password"))
			.dailyCheckEnabled(true)  // 안부 메시지 수신
			.build();
		soonja = memberRepository.save(soonja);
		soonjaToken = jwtUtil.createAccessToken(String.valueOf(soonja.getId()), soonja.getMemberEmail());

		// 김영희 (보호자 역할)
		younghee = MemberEntity.builder()
			.memberEmail("younghee@example.com")
			.memberName("김영희")
			.memberPassword(passwordEncoder.encode("password"))
			.dailyCheckEnabled(false)  // 안부 메시지 미수신
			.build();
		younghee = memberRepository.save(younghee);
		youngheeToken = jwtUtil.createAccessToken(String.valueOf(younghee.getId()), younghee.getMemberEmail());
	}

	/**
	 * Journey 1: 첫 시작 - 김순자 할머니 회원가입 및 초기 상태 확인
	 *
	 * 시나리오:
	 * 1. 김순자 할머니가 MARUNI 앱 설치
	 * 2. 회원가입 (이미 setUp()에서 완료)
	 * 3. 내 정보 조회 - dailyCheckEnabled=true, guardian=null
	 */
	@Test
	@DisplayName("Journey 1: 김순자 할머니 회원가입 및 초기 상태 확인")
	void journey1_soonjaSignup_initialState() throws Exception {
		// When: 김순자 할머니가 내 정보 조회
		mockMvc.perform(get("/api/members/me")
				.header("Authorization", "Bearer " + soonjaToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("M209"))
			.andExpect(jsonPath("$.data.memberName").value("김순자"))
			.andExpect(jsonPath("$.data.memberEmail").value("soonja@example.com"))
			.andExpect(jsonPath("$.data.dailyCheckEnabled").value(true))
			.andExpect(jsonPath("$.data.guardian").doesNotExist())
			.andExpect(jsonPath("$.data.managedMembers").isEmpty())
			.andDo(print());

		// Then: DB 검증
		MemberEntity member = memberRepository.findById(soonja.getId()).get();
		assertThat(member.getDailyCheckEnabled()).isTrue();
		assertThat(member.getGuardian()).isNull();
		assertThat(member.getManagedMembers()).isEmpty();
	}

	/**
	 * Journey 3: 보호자 등록 - 김순자 할머니가 김영희에게 보호자 요청
	 *
	 * 시나리오:
	 * 1. 김순자가 설정 메뉴에서 보호자 찾기
	 * 2. 김영희 이메일로 검색
	 * 3. 보호자 등록 요청 생성
	 * 4. 요청 상태 PENDING 확인
	 */
	@Test
	@DisplayName("Journey 3: 김순자가 김영희에게 보호자 요청")
	void journey3_soonjaRequestsGuardian_toYounghee() throws Exception {
		// Given: 보호자 요청 DTO
		GuardianRequestDto request = new GuardianRequestDto();
		request.setGuardianId(younghee.getId());
		request.setRelation(GuardianRelation.FAMILY);

		// When: 김순자가 김영희에게 보호자 요청
		mockMvc.perform(post("/api/guardians/requests")
				.header("Authorization", "Bearer " + soonjaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("GR201"))
			.andExpect(jsonPath("$.data.requester.name").value("김순자"))
			.andExpect(jsonPath("$.data.guardian.name").value("김영희"))
			.andExpect(jsonPath("$.data.relation").value("FAMILY"))
			.andExpect(jsonPath("$.data.status").value("PENDING"))
			.andDo(print());

		// Then: DB 검증 - GuardianRequest가 생성되었는지 확인
		assertThat(guardianRequestRepository.findAll()).hasSize(1);
	}

	/**
	 * Journey 3 → Journey 4: 김순자 검색 후 보호자 요청, 김영희가 요청 확인
	 *
	 * 시나리오:
	 * 1. 김순자가 회원 검색 (이메일 기반)
	 * 2. 김영희 검색 결과 확인
	 * 3. 보호자 요청 생성
	 * 4. 김영희가 받은 요청 목록 조회
	 */
	@Test
	@DisplayName("Journey 3-4: 김순자가 김영희 검색 후 보호자 요청, 김영희가 요청 확인")
	void journey3to4_searchAndRequest_guardianReceivesNotification() throws Exception {
		// Step 1: 김순자가 김영희 이메일로 회원 검색
		mockMvc.perform(get("/api/members/search")
				.header("Authorization", "Bearer " + soonjaToken)
				.param("email", "younghee@example.com"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("M209"))
			.andExpect(jsonPath("$.data.memberName").value("김영희"))
			.andExpect(jsonPath("$.data.memberEmail").value("younghee@example.com"))
			.andDo(print());

		// Step 2: 김순자가 보호자 요청 생성
		GuardianRequestDto request = new GuardianRequestDto();
		request.setGuardianId(younghee.getId());
		request.setRelation(GuardianRelation.FAMILY);

		mockMvc.perform(post("/api/guardians/requests")
				.header("Authorization", "Bearer " + soonjaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("GR201"))
			.andDo(print());

		// Step 3: 김영희가 받은 요청 목록 조회
		mockMvc.perform(get("/api/guardians/requests")
				.header("Authorization", "Bearer " + youngheeToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("GR202"))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data", hasSize(1)))
			.andExpect(jsonPath("$.data[0].requester.name").value("김순자"))
			.andExpect(jsonPath("$.data[0].status").value("PENDING"))
			.andDo(print());
	}

	/**
	 * Journey 4: 보호자 요청 수락 - 김영희가 김순자의 요청을 수락
	 *
	 * 시나리오:
	 * 1. 김순자가 보호자 요청 생성
	 * 2. 김영희가 요청 수락
	 * 3. 관계 설정 완료 확인 (김순자.guardian = 김영희)
	 * 4. 김영희의 managedMembers에 김순자 추가 확인
	 */
	@Test
	@DisplayName("Journey 4: 김영희가 김순자의 보호자 요청을 수락하고 관계 설정")
	void journey4_youngheeAcceptsRequest_relationshipEstablished() throws Exception {
		// Given: 김순자가 보호자 요청 생성
		GuardianRequestDto request = new GuardianRequestDto();
		request.setGuardianId(younghee.getId());
		request.setRelation(GuardianRelation.FAMILY);

		String response = mockMvc.perform(post("/api/guardians/requests")
				.header("Authorization", "Bearer " + soonjaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andReturn().getResponse().getContentAsString();

		// requestId 추출
		Long requestId = objectMapper.readTree(response).get("data").get("id").asLong();

		// When: 김영희가 요청 수락
		mockMvc.perform(post("/api/guardians/requests/{requestId}/accept", requestId)
				.header("Authorization", "Bearer " + youngheeToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("GR203"))
			.andDo(print());

		// Then: 김순자의 보호자가 김영희로 설정되었는지 확인
		mockMvc.perform(get("/api/members/me")
				.header("Authorization", "Bearer " + soonjaToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.guardian.memberName").value("김영희"))
			.andDo(print());

		// Then: 김영희의 managedMembers에 김순자가 추가되었는지 확인
		mockMvc.perform(get("/api/members/me/managed-members")
				.header("Authorization", "Bearer " + youngheeToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("M209"))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data", hasSize(1)))
			.andExpect(jsonPath("$.data[0].memberName").value("김순자"))
			.andDo(print());

		// DB 검증 - 김순자의 보호자 설정 확인
		MemberEntity updatedSoonja = memberRepository.findById(soonja.getId()).get();
		assertThat(updatedSoonja.getGuardian()).isNotNull();
		assertThat(updatedSoonja.getGuardian().getId()).isEqualTo(younghee.getId());
	}

	/**
	 * Journey 완전 플로우: 회원가입 → 보호자 검색 → 요청 → 수락 → 관계 해제
	 *
	 * 전체 생명주기를 검증합니다.
	 */
	@Test
	@DisplayName("전체 플로우: 보호자 요청 → 수락 → 관계 확인 → 관계 해제")
	void fullJourney_requestAcceptAndRemove() throws Exception {
		// Step 1: 김순자가 보호자 요청
		GuardianRequestDto request = new GuardianRequestDto();
		request.setGuardianId(younghee.getId());
		request.setRelation(GuardianRelation.FAMILY);

		String response = mockMvc.perform(post("/api/guardians/requests")
				.header("Authorization", "Bearer " + soonjaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		Long requestId = objectMapper.readTree(response).get("data").get("id").asLong();

		// Step 2: 김영희가 요청 수락
		mockMvc.perform(post("/api/guardians/requests/{requestId}/accept", requestId)
				.header("Authorization", "Bearer " + youngheeToken))
			.andExpect(status().isOk())
			.andDo(print());

		// Step 3: 관계 설정 확인
		MemberEntity updatedSoonja = memberRepository.findById(soonja.getId()).get();
		assertThat(updatedSoonja.getGuardian()).isNotNull();

		// Step 4: 김순자가 보호자 관계 해제
		mockMvc.perform(delete("/api/members/me/guardian")
				.header("Authorization", "Bearer " + soonjaToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("GR205"))
			.andDo(print());

		// Step 5: 관계 해제 확인
		MemberEntity finalSoonja = memberRepository.findById(soonja.getId()).get();
		assertThat(finalSoonja.getGuardian()).isNull();
	}

	/**
	 * 에러 케이스: 중복 요청 방지
	 *
	 * 김순자가 김영희에게 이미 PENDING 요청이 있는 상태에서 다시 요청하면 실패
	 */
	@Test
	@DisplayName("에러 케이스: 중복 보호자 요청 방지")
	void errorCase_duplicateRequest_shouldFail() throws Exception {
		// Given: 첫 번째 요청 생성
		GuardianRequestDto request = new GuardianRequestDto();
		request.setGuardianId(younghee.getId());
		request.setRelation(GuardianRelation.FAMILY);

		mockMvc.perform(post("/api/guardians/requests")
				.header("Authorization", "Bearer " + soonjaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(print());

		// When: 중복 요청 시도
		mockMvc.perform(post("/api/guardians/requests")
				.header("Authorization", "Bearer " + soonjaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().is4xxClientError())
			.andDo(print());
	}

	/**
	 * 에러 케이스: 자기 자신에게 보호자 요청 불가
	 */
	@Test
	@DisplayName("에러 케이스: 자기 자신에게 보호자 요청 불가")
	void errorCase_selfRequest_shouldFail() throws Exception {
		// Given: 자기 자신에게 요청
		GuardianRequestDto request = new GuardianRequestDto();
		request.setGuardianId(soonja.getId());  // 자기 자신
		request.setRelation(GuardianRelation.FAMILY);

		// When & Then: 실패 예상
		mockMvc.perform(post("/api/guardians/requests")
				.header("Authorization", "Bearer " + soonjaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().is4xxClientError())
			.andDo(print());
	}

	/**
	 * 보호자 요청 거절 시나리오
	 *
	 * 김영희가 김순자의 요청을 거절
	 */
	@Test
	@DisplayName("보호자 요청 거절 시나리오")
	void journey_rejectRequest() throws Exception {
		// Given: 김순자가 보호자 요청
		GuardianRequestDto request = new GuardianRequestDto();
		request.setGuardianId(younghee.getId());
		request.setRelation(GuardianRelation.FAMILY);

		String response = mockMvc.perform(post("/api/guardians/requests")
				.header("Authorization", "Bearer " + soonjaToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andReturn().getResponse().getContentAsString();

		Long requestId = objectMapper.readTree(response).get("data").get("id").asLong();

		// When: 김영희가 요청 거절
		mockMvc.perform(post("/api/guardians/requests/{requestId}/reject", requestId)
				.header("Authorization", "Bearer " + youngheeToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("GR204"))
			.andDo(print());

		// Then: 관계가 설정되지 않았는지 확인
		MemberEntity updatedSoonja = memberRepository.findById(soonja.getId()).get();
		assertThat(updatedSoonja.getGuardian()).isNull();
	}
}
