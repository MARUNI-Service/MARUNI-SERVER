package com.anyang.maruni.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
    "com.anyang.maruni.domain.member.domain.repository",
    "com.anyang.maruni.domain.conversation.domain.repository",
    "com.anyang.maruni.domain.dailycheck.domain.repository",
    "com.anyang.maruni.domain.guardian.domain.repository",
    "com.anyang.maruni.domain.alertrule.domain.repository",
    "com.anyang.maruni.domain.notification.domain.repository"
})
public class JpaConfig {
}