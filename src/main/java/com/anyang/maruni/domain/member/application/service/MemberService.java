package com.anyang.maruni.domain.member.application.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anyang.maruni.domain.member.application.dto.request.MemberSaveRequest;
import com.anyang.maruni.domain.member.application.dto.request.MemberUpdateRequest;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.application.mapper.MemberMapper;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.global.exception.BaseException;
import com.anyang.maruni.global.response.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용
public class MemberService {
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final MemberMapper memberMapper;


	@Transactional // 쓰기 작업
	public MemberResponse save(MemberSaveRequest req) {
		boolean exists = memberRepository.findByMemberEmail(req.getMemberEmail()).isPresent();
		if (exists) {
			throw new BaseException(ErrorCode.DUPLICATE_EMAIL);
		}

		String encodedPassword = passwordEncoder.encode(req.getMemberPassword());

		log.info("[회원가입] 이메일: {}, 이름: {}", req.getMemberEmail(), req.getMemberName());

		MemberEntity memberEntity = memberMapper.toEntity(req, encodedPassword);
		MemberEntity savedEntity = memberRepository.save(memberEntity);
		return memberMapper.toResponse(savedEntity);
	}


	public List<MemberResponse> findAll() {
		List<MemberEntity> entities = memberRepository.findAll();
		return memberMapper.toResponseList(entities);
	}

	public MemberResponse findById(Long id) {
		MemberEntity entity = memberRepository.findById(id)
			.orElseThrow(() -> memberNotFound());
		return memberMapper.toResponse(entity);
	}

	@Transactional // 쓰기 작업
	public void update(MemberUpdateRequest req) {
		MemberEntity entity = memberRepository.findById(req.getId())
			.orElseThrow(() -> memberNotFound());

		String encodedPassword = passwordEncoder.encode(req.getMemberPassword());

		entity.updateMemberInfo(req.getMemberName(), encodedPassword);
		memberRepository.save(entity);
	}

	@Transactional // 쓰기 작업
	public void deleteById(Long id) {
		if (!memberRepository.existsById(id)) {
			throw memberNotFound();
		}
		memberRepository.deleteById(id);
	}

	public boolean isEmailAvailable(String memberEmail) {
		return !memberRepository.existsByMemberEmail(memberEmail);
	}

	// ========== 신규 메서드 (Phase 1) ==========

	/**
	 * 회원 검색 (이메일 기반)
	 */
	public MemberResponse searchByEmail(String email) {
		MemberEntity member = memberRepository.searchByEmail(email)
			.orElseThrow(() -> new BaseException(ErrorCode.MEMBER_NOT_FOUND));

		return memberMapper.toResponse(member);
	}

	/**
	 * 내가 돌보는 사람들 목록 조회
	 */
	public List<MemberResponse> getManagedMembers(Long guardianId) {
		MemberEntity guardian = memberRepository.findById(guardianId)
			.orElseThrow(() -> memberNotFound());

		List<MemberEntity> managedMembers = memberRepository.findByGuardian(guardian);

		return memberMapper.toResponseList(managedMembers);
	}

	/**
	 * 안부 메시지 설정 변경
	 */
	@Transactional
	public MemberResponse updateDailyCheckEnabled(Long memberId, Boolean enabled) {
		MemberEntity member = memberRepository.findById(memberId)
			.orElseThrow(() -> memberNotFound());

		member.updateDailyCheckEnabled(enabled);
		memberRepository.save(member);

		log.info("Daily check enabled updated: memberId={}, enabled={}", memberId, enabled);

		return memberMapper.toResponse(member);
	}

	/**
	 * 내 프로필 조회 (역할 정보 포함)
	 * API 명세서의 GET /members/me 응답 형식
	 */
	public MemberResponse getMyProfile(Long memberId) {
		MemberEntity member = memberRepository.findById(memberId)
			.orElseThrow(() -> memberNotFound());

		// MemberResponse에 guardian, managedMembers 정보 포함
		return MemberResponse.fromWithRoles(member);
	}

	// ========== Private Helper Methods ==========

	private BaseException memberNotFound() {
		return new BaseException(ErrorCode.MEMBER_NOT_FOUND);
	}
}