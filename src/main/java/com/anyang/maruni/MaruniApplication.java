package com.anyang.maruni;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MaruniApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaruniApplication.class, args);
	}

}
