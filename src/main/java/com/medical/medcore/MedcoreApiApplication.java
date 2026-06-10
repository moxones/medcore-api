package com.medical.medcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedcoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedcoreApiApplication.class, args);
	}

}
