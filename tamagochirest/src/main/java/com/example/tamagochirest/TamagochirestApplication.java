package com.example.tamagochirest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@SpringBootApplication(
	scanBasePackages = {"com.example.tamagochirest", "com.example.tamagochi_api_contract", "com.example.events"},
	exclude = {DataSourceAutoConfiguration.class}
)
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class TamagochirestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TamagochirestApplication.class, args);
	}

}
