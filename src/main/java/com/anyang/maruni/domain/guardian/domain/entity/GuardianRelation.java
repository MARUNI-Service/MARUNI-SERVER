package com.anyang.maruni.domain.guardian.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GuardianRelation {
	FAMILY("가족"),
	FRIEND("지인"),
	CAREGIVER("간병인"),
	MEDICAL_STAFF("의료진"),
	OTHER("기타");

	private final String displayName;
}
