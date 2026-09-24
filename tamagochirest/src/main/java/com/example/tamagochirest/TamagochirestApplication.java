package com.example.tamagochirest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@SpringBootApplication(
	scanBasePackages = {"com.example.tamagochirest", "com.example.tamagochi_api_contract", "com.example.events"}
)
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@EnableJpaRepositories(basePackages = "com.example.tamagochirest.repository")
public class TamagochirestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TamagochirestApplication.class, args);
	}

}
