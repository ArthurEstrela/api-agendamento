package com.stylo.api_agendamento.config;

import com.stylo.api_agendamento.core.ports.*;
import com.stylo.api_agendamento.core.usecases.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfiguration {

    // --- DOMÍNIO: AGENDAMENTOS (APPOINTMENTS) ---

    @Bean
    public CreateAppointmentUseCase createAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IProfessionalRepository professionalRepository,
            IServiceRepository serviceRepository,
            IUserRepository userRepository) {
        return new CreateAppointmentUseCase(
                appointmentRepository,
                professionalRepository,
                serviceRepository,
                userRepository);
    }

    @Bean
    public CreateManualAppointmentUseCase createManualAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IProfessionalRepository professionalRepository,
            IServiceRepository serviceRepository) {
        return new CreateManualAppointmentUseCase(
                appointmentRepository,
                professionalRepository,
                serviceRepository);
    }

    @Bean
    public ConfirmAppointmentUseCase confirmAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IProfessionalRepository professionalRepository,
            INotificationProvider notificationProvider) { // ✨ Novo
        return new ConfirmAppointmentUseCase(appointmentRepository, professionalRepository, notificationProvider);
    }

    @Bean
    public CompleteAppointmentUseCase completeAppointmentUseCase(IAppointmentRepository repository,
            IFinancialRepository financialRepository) {
        return new CompleteAppointmentUseCase(repository, financialRepository);
    }

    @Bean
    public CancelAppointmentUseCase cancelAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IUserRepository userRepository, // ✨ Novo (para buscar o prof)
            INotificationProvider notificationProvider) { // ✨ Novo
        return new CancelAppointmentUseCase(appointmentRepository, userRepository, notificationProvider);
    }

    @Bean
    public RescheduleAppointmentUseCase rescheduleAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IProfessionalRepository professionalRepository,
            IServiceProviderRepository serviceProviderRepository,
            INotificationProvider notificationProvider) {
        return new RescheduleAppointmentUseCase(appointmentRepository, professionalRepository,
                serviceProviderRepository,
                notificationProvider);
    }

    @Bean
    public GetAvailableSlotsUseCase getAvailableSlotsUseCase(
            IProfessionalRepository professionalRepository,
            IAppointmentRepository appointmentRepository) {
        return new GetAvailableSlotsUseCase(professionalRepository, appointmentRepository);
    }

    // --- DOMÍNIO: PROFISSIONAIS E SERVIÇOS ---

    @Bean
    public UpdateProfessionalAvailabilityUseCase updateProfessionalAvailabilityUseCase(
            IProfessionalRepository repository) {
        return new UpdateProfessionalAvailabilityUseCase(repository);
    }

    @Bean
    public GetProfessionalProfileUseCase getProfessionalProfileUseCase(
            IProfessionalRepository professionalRepository,
            IReviewRepository reviewRepository) {
        return new GetProfessionalProfileUseCase(professionalRepository, reviewRepository);
    }

    @Bean
    public CreateServiceUseCase createServiceUseCase(IServiceRepository repository) {
        return new CreateServiceUseCase(repository);
    }

    @Bean
    public BlockProfessionalTimeUseCase blockProfessionalTimeUseCase(IProfessionalRepository professionalRepository,
            IAppointmentRepository repository) {
        return new BlockProfessionalTimeUseCase(professionalRepository, repository);
    }

    // --- DOMÍNIO: ESTABELECIMENTO (SERVICE PROVIDER) ---

    @Bean
    public RegisterServiceProviderUseCase registerServiceProviderUseCase(
            IServiceProviderRepository providerRepository,
            IProfessionalRepository professionalRepository,
            IUserRepository userRepository) {
        return new RegisterServiceProviderUseCase(providerRepository, professionalRepository, userRepository);
    }

    // --- DOMÍNIO: FINANCEIRO E AVALIAÇÕES ---

    @Bean
    public GetFinancialDashboardUseCase getFinancialDashboardUseCase(
            IAppointmentRepository appointmentRepository,
            IFinancialRepository financialRepository) {
        return new GetFinancialDashboardUseCase(appointmentRepository, financialRepository);
    }

    @Bean
    public CreateReviewUseCase createReviewUseCase(
            IReviewRepository reviewRepository,
            IAppointmentRepository appointmentRepository) {
        return new CreateReviewUseCase(reviewRepository, appointmentRepository);
    }

    // --- DOMÍNIO: SISTEMA (NOTIFICAÇÕES E PAGAMENTOS) ---

    @Bean
    public SendRemindersUseCase sendRemindersUseCase(
            IAppointmentRepository appointmentRepository,
            INotificationProvider notificationProvider) {
        return new SendRemindersUseCase(appointmentRepository, notificationProvider);
    }

    @Bean
    public HandlePaymentWebhookUseCase handlePaymentWebhookUseCase(
            IServiceProviderRepository serviceProviderRepository, // Ajustado o nome
            IPaymentProvider paymentProvider) {
        return new HandlePaymentWebhookUseCase(serviceProviderRepository, paymentProvider);
    }

    @Bean
    public UpdateFcmTokenUseCase updateFcmTokenUseCase(IUserRepository userRepository) {
        return new UpdateFcmTokenUseCase(userRepository);
    }

    @Bean
public RequestPasswordResetUseCase requestPasswordResetUseCase(
        IUserRepository userRepository, 
        INotificationProvider notificationProvider) {
    return new RequestPasswordResetUseCase(userRepository, notificationProvider);
}

@Bean
public ResetPasswordUseCase resetPasswordUseCase(
        IUserRepository userRepository, 
        PasswordEncoder passwordEncoder) {
    return new ResetPasswordUseCase(userRepository, passwordEncoder);
}
}