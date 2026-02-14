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
            IUserRepository userRepository,
            IServiceProviderRepository serviceProviderRepository, // <--- Novo parametro
            IEventPublisher eventPublisher) {
        return new CreateAppointmentUseCase(
                appointmentRepository,
                professionalRepository,
                serviceRepository,
                userRepository,
                serviceProviderRepository, // <--- Passando pro construtor
                eventPublisher);
    }

    @Bean
    public CreateManualAppointmentUseCase createManualAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IProfessionalRepository professionalRepository,
            IServiceRepository serviceRepository,
            IServiceProviderRepository serviceProviderRepository, // ✨ Nova dependência
            IEventPublisher eventPublisher // ✨ Nova dependência
    ) {
        return new CreateManualAppointmentUseCase(
                appointmentRepository,
                professionalRepository,
                serviceRepository,
                serviceProviderRepository,
                eventPublisher);
    }

    @Bean
    public ConfirmAppointmentUseCase confirmAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IProfessionalRepository professionalRepository,
            INotificationProvider notificationProvider) { // ✨ Novo
        return new ConfirmAppointmentUseCase(appointmentRepository, professionalRepository, notificationProvider);
    }

    @Bean
    public CompleteAppointmentUseCase completeAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IProfessionalRepository professionalRepository,
            IServiceProviderRepository serviceProviderRepository,
            IProductRepository productRepository,
            IFinancialRepository financialRepository,
            INotificationProvider notificationProvider) {

        return new CompleteAppointmentUseCase(
                appointmentRepository,
                professionalRepository,
                serviceProviderRepository,
                productRepository,
                financialRepository,
                notificationProvider);
    }

    @Bean
    public CancelAppointmentUseCase cancelAppointmentUseCase(
            IAppointmentRepository appointmentRepository,
            IServiceProviderRepository providerRepository,
            IUserRepository userRepository,
            INotificationProvider notificationProvider,
            IPaymentProvider paymentProvider) {
        return new CancelAppointmentUseCase(
                appointmentRepository,
                providerRepository,
                userRepository,
                notificationProvider,
                paymentProvider);
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
    public BlockProfessionalTimeUseCase blockProfessionalTimeUseCase(
            IProfessionalRepository professionalRepository,
            IAppointmentRepository appointmentRepository,
            IServiceProviderRepository serviceProviderRepository, // ✨ Nova dependência
            IEventPublisher eventPublisher // ✨ Nova dependência (Opcional, mas boa prática)
    ) {
        return new BlockProfessionalTimeUseCase(
                professionalRepository,
                appointmentRepository,
                serviceProviderRepository,
                eventPublisher);
    }

    // --- DOMÍNIO: ESTABELECIMENTO (SERVICE PROVIDER) ---

    @Bean
    public RegisterServiceProviderUseCase registerServiceProviderUseCase(
            IServiceProviderRepository providerRepository,
            IProfessionalRepository professionalRepository,
            IUserRepository userRepository,
            INotificationProvider notificationProvider) {
        return new RegisterServiceProviderUseCase(
                providerRepository,
                professionalRepository,
                userRepository,
                notificationProvider);
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

    @Bean
    public UpdateProfessionalCommissionUseCase updateProfessionalCommissionUseCase(IProfessionalRepository repository) {
        return new UpdateProfessionalCommissionUseCase(repository);
    }

    @Bean
    public UpdateServiceUseCase updateServiceUseCase(IServiceRepository repository) {
        return new UpdateServiceUseCase(repository);
    }

    @Bean
    public GetClientHistoryUseCase getClientHistoryUseCase(IAppointmentRepository appointmentRepository) {
        return new GetClientHistoryUseCase(appointmentRepository);
    }

    @Bean
    public GetProfessionalAvailabilityUseCase getProfessionalAvailabilityUseCase(
            IProfessionalRepository professionalRepository,
            IAppointmentRepository appointmentRepository,
            IServiceRepository serviceRepository) {
        return new GetProfessionalAvailabilityUseCase(
                professionalRepository,
                appointmentRepository,
                serviceRepository);
    }

    @Bean
    public GetOccupancyReportUseCase getOccupancyReportUseCase(
            IProfessionalRepository professionalRepository,
            IAppointmentRepository appointmentRepository) {
        return new GetOccupancyReportUseCase(professionalRepository, appointmentRepository);
    }

    @Bean
    public ProcessSubscriptionStatusUseCase processSubscriptionStatusUseCase(
            IServiceProviderRepository providerRepository,
            INotificationProvider notificationProvider) {
        return new ProcessSubscriptionStatusUseCase(providerRepository, notificationProvider);
    }

    @Bean
    public SendPendingRemindersUseCase sendPendingRemindersUseCase(
            IAppointmentRepository appointmentRepository,
            INotificationProvider notificationProvider,
            IUserRepository userRepository) {
        return new SendPendingRemindersUseCase(
                appointmentRepository,
                notificationProvider,
                userRepository);
    }

    @Bean
    public MarkNoShowUseCase markNoShowUseCase(IAppointmentRepository appointmentRepository) {
        return new MarkNoShowUseCase(appointmentRepository);
    }

    @Bean
    public CloseProfessionalPeriodUseCase closeProfessionalPeriodUseCase(
            IAppointmentRepository appointmentRepository,
            IFinancialRepository financialRepository) {
        return new CloseProfessionalPeriodUseCase(appointmentRepository, financialRepository);
    }

    @Bean
    public ProcessAutomaticSplitUseCase processAutomaticSplitUseCase(
            IProfessionalRepository professionalRepository,
            IAppointmentRepository appointmentRepository, // ✨ Adicionado
            IPaymentProvider paymentProvider) {
        return new ProcessAutomaticSplitUseCase(
                professionalRepository,
                appointmentRepository,
                paymentProvider);
    }
}