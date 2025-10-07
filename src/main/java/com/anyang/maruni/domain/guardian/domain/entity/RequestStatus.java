package com.anyang.maruni.domain.guardian.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 보호자 요청 상태
 *
 * PENDING: 대기 중 (수락/거절 대기)
 * ACCEPTED: 수락됨
 * REJECTED: 거절됨
 */
@Getter
@RequiredArgsConstructor
public enum RequestStatus {
	PENDING("대기중"),
	ACCEPTED("수락됨"),
	REJECTED("거절됨");

	private final String displayName;
}
