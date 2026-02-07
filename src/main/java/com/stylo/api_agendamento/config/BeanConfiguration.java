package com.stylo.api_agendamento.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stylo.api_agendamento.adapters.outbound.persistence.user.UserPersistenceAdapter;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.usecases.CreateAppointmentUseCase;

@Configuration
public class BeanConfiguration {
    @Bean
    public CreateAppointmentUseCase createAppointmentUseCase(
            IAppointmentRepository repository,
            INotificationProvider notificationProvider) {
        return new CreateAppointmentUseCase(repository, notificationProvider);
    }

    @Bean
    public IUserRepository userRepository(UserPersistenceAdapter userPersistenceAdapter) {
        return userPersistenceAdapter;
    }
}