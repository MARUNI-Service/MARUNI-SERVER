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
	public void save(MemberSaveRequest req) {
		boolean exists = memberRepository.findByMemberEmail(req.getMemberEmail()).isPresent();
		if (exists) {
			throw new BaseException(ErrorCode.DUPLICATE_EMAIL);
		}

		String encodedPassword = passwordEncoder.encode(req.getMemberPassword());

		log.info("[회원가입] 이메일: {}, 이름: {}", req.getMemberEmail(), req.getMemberName());

		MemberEntity memberEntity = memberMapper.toEntity(req, encodedPassword);
		memberRepository.save(memberEntity);
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

	private BaseException memberNotFound() {
		return new BaseException(ErrorCode.MEMBER_NOT_FOUND);
	}
}