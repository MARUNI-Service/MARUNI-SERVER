package com.anyang.maruni.domain.notification.infrastructure.service;

import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.domain.member.domain.repository.MemberRepository;
import com.anyang.maruni.domain.notification.domain.exception.NotificationException;
import com.anyang.maruni.domain.notification.domain.service.PushTokenService;
import com.anyang.maruni.global.response.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 푸시 토큰 서비스 구현체
 *
 * MemberRepository를 통해 실제 회원의 푸시 토큰을 조회합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PushTokenServiceImpl implements PushTokenService {

    private final MemberRepository memberRepository;

    @Override
    public String getPushTokenByMemberId(Long memberId) {
        validateMemberId(memberId);

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.error("Member not found for push token lookup: {}", memberId);
                    return new NotificationException(ErrorCode.MEMBER_NOT_FOUND);
                });

        String pushToken = member.getPushToken();

        if (pushToken == null || pushToken.trim().isEmpty()) {
            log.warn("Member {} does not have a push token", memberId);
            throw new NotificationException(ErrorCode.PUSH_TOKEN_INVALID);
        }

        log.debug("Retrieved push token for member {}: {}...{}",
                memberId,
                pushToken.substring(0, Math.min(pushToken.length(), 10)),
                pushToken.substring(Math.max(0, pushToken.length() - 10)));

        return pushToken;
    }

    @Override
    public boolean hasPushToken(Long memberId) {
        validateMemberId(memberId);

        return memberRepository.findById(memberId)
                .map(MemberEntity::hasPushToken)
                .orElse(false);
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("Invalid member ID: " + memberId);
        }
    }
}