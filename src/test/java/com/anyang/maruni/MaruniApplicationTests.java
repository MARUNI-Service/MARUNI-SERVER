package com.anyang.maruni;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.anyang.maruni.global.config.TestRedisConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class MaruniApplicationTests {

	@Test
	void contextLoads() {
	}

}
