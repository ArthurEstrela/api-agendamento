package com.stylo.api_agendamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ApiAgendamentoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiAgendamentoApplication.class, args);
	}

}
