package com.stylo.api_agendamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

import com.stylo.api_agendamento.core.common.UseCase;

@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = "com.stylo.api_agendamento", includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = UseCase.class))
@EnableCaching
public class ApiAgendamentoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiAgendamentoApplication.class, args);
	}

}
