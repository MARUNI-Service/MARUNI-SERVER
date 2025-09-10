package com.anyang.maruni.domain.auth.domain.repository;

import org.springframework.data.repository.CrudRepository;

import com.anyang.maruni.domain.auth.domain.entity.RefreshToken;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
