package com.anyang.maruni.domain.member.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.anyang.maruni.domain.member.application.service.MemberService;
import com.anyang.maruni.domain.member.domain.entity.MemberEntity;
import com.anyang.maruni.global.exception.BaseException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final MemberService memberService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			MemberEntity member = memberService.findByEmail(username);
			return new CustomUserDetails(member);
		} catch (BaseException e) {
			throw new UsernameNotFoundException("해당 이메일의 회원을 찾을 수 없습니다: " + username, e);
		}
	}
}