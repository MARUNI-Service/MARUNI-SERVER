package com.anyang.maruni.domain.guardian.application.mapper;

import com.anyang.maruni.domain.guardian.application.dto.GuardianResponseDto;
import com.anyang.maruni.domain.guardian.domain.entity.GuardianEntity;
import com.anyang.maruni.domain.member.application.dto.response.MemberResponse;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Guardian 도메인 매핑 컴포넌트
 *
 * 단일 책임: Entity ↔ DTO 변환 로직 전담
 * - Guardian Entity to DTO 변환
 * - Member Entity to DTO 변환 (Guardian 컨텍스트)
 * - 컬렉션 변환 로직
 */
@Component
public class GuardianMapper {

    /**
     * GuardianEntity를 GuardianResponseDto로 변환
     *
     * @param entity 변환할 Guardian 엔티티
     * @return Guardian 응답 DTO
     */
    public GuardianResponseDto toResponseDto(GuardianEntity entity) {
        return GuardianResponseDto.from(entity);
    }

    /**
     * MemberEntity 리스트를 MemberResponse 리스트로 변환
     *
     * @param members 변환할 Member 엔티티 리스트
     * @return Member 응답 DTO 리스트
     */
    public List<MemberResponse> toMemberResponseList(List<MemberEntity> members) {
        return members.stream()
            .map(MemberResponse::from)
            .toList();
    }

    /**
     * 단일 MemberEntity를 MemberResponse로 변환
     *
     * @param member 변환할 Member 엔티티
     * @return Member 응답 DTO
     */
    public MemberResponse toMemberResponse(MemberEntity member) {
        return MemberResponse.from(member);
    }
}