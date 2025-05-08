package com.example.znsk_shortener_url;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZnskShortenerUrlApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZnskShortenerUrlApplication.class, args);

	}

}
