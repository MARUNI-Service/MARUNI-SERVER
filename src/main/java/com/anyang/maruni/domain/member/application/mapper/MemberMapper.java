package com.anyang.maruni.domain.member.application.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.anyang.maruni.domain.member.application.dto.request.MemberSaveRequest;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;

@Component
public class MemberMapper {

	/**
	 * MemberEntity를 MemberResponse로 변환 (기본)
	 */
	public MemberResponse toResponse(MemberEntity entity) {
		return MemberResponse.from(entity);
	}

	/**
	 * MemberEntity를 MemberResponse로 변환 (역할 정보 포함)
	 */
	public MemberResponse toResponseWithRoles(MemberEntity entity) {
		return MemberResponse.fromWithRoles(entity);
	}

	/**
	 * MemberEntity 리스트를 MemberResponse 리스트로 변환
	 */
	public List<MemberResponse> toResponseList(List<MemberEntity> entities) {
		return entities.stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * MemberSaveRequest를 MemberEntity로 변환 (Phase 1 수정)
	 * 주의: 비밀번호 암호화는 Service 레이어에서 처리
	 */
	public MemberEntity toEntity(MemberSaveRequest request, String encodedPassword) {
		return MemberEntity.createMember(
			request.getMemberEmail(),
			request.getMemberName(),
			encodedPassword,
			request.getDailyCheckEnabled()
		);
	}
}