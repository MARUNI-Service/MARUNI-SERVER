package com.anyang.maruni.domain.member.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.oauth2.domain.entity.SocialType;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

	Optional<MemberEntity> findByMemberEmail(String memberEmail);


	Boolean existsByMemberEmail(String memberEmail);

	Optional<MemberEntity> findBySocialTypeAndSocialId(SocialType socialType, String socialId);
}