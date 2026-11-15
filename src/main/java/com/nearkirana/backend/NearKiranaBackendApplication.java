package com.nearkirana.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // finds @ConfigurationProperties classes such as OtpProperties
public class NearKiranaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NearKiranaBackendApplication.class, args);
	}

}
