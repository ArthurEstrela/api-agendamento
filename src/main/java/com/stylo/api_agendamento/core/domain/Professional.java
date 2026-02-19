package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Professional {

    private UUID id;
    private UUID serviceProviderId; // Vinculo com o Salão/Empresa

    // Nota: Removi serviceProviderName para evitar duplicação de dados
    // (normalização).
    // O nome do salão deve ser obtido via repositório ou projeção (DTO) quando
    // necessário.

    private String name;
    private String email;
    private String avatarUrl;
    private String bio;

    // ID externo do Stripe/Pagar.me para split de pagamento
    private String gatewayAccountId;

    // --- CONFIGURAÇÃO FINANCEIRA ---
    private RemunerationType remunerationType;
    private BigDecimal remunerationValue;

    // ✨ CAMPO ADICIONADO: Especialidades (Tags do perfil)
    @Builder.Default
    private List<String> specialties = new ArrayList<>();

    // --- AGREGADOS ---
    @Builder.Default
    private List<Service> services = new ArrayList<>();

    @Builder.Default
    private List<DailyAvailability> availability = new ArrayList<>();

    private Integer slotInterval; // Ex: 30 minutos
    private boolean isOwner; // Se é o dono do salão (pode ter permissões extras)

    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Professional create(String name, String email, UUID providerId,
            List<Service> services, List<DailyAvailability> availability) {

        if (name == null || name.isBlank())
            throw new BusinessException("Nome do profissional é obrigatório.");
        if (email == null || !email.contains("@"))
            throw new BusinessException("E-mail inválido.");
        if (providerId == null)
            throw new BusinessException("O profissional deve estar vinculado a um estabelecimento.");

        // Validação de Serviços (Regra de Negócio: Profissional deve fazer algo)
        if (services == null || services.isEmpty()) {
            throw new BusinessException("Um profissional deve estar habilitado em pelo menos um serviço.");
        }

        // Validação de Disponibilidade
        if (availability == null || availability.isEmpty()) {
            throw new BusinessException("O profissional deve ter pelo menos um dia de disponibilidade configurado.");
        }

        return Professional.builder()
                .id(UUID.randomUUID()) // Identidade gerada
                .name(name)
                .email(email)
                .serviceProviderId(providerId)
                .services(new ArrayList<>(services)) // Lista mutável
                .availability(new ArrayList<>(availability)) // Lista mutável
                .slotInterval(30) // Default
                .isOwner(false)
                .isActive(true)
                // Padrão: Sem comissão definida inicialmente
                .remunerationType(null)
                .remunerationValue(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- REGRAS DE NEGÓCIO: DISPONIBILIDADE ---

    public boolean isAvailable(LocalDateTime dateTime, int totalDurationMinutes) {
        if (!this.isActive)
            return false;

        return availability.stream()
                .filter(a -> a.dayOfWeek() == dateTime.getDayOfWeek())
                .findFirst()
                .map(a -> a.isOpen() && a.contains(dateTime.toLocalTime(), totalDurationMinutes))
                .orElse(false);
    }

    public void updateAvailability(List<DailyAvailability> newAvailability) {
        if (newAvailability == null || newAvailability.isEmpty()) {
            throw new BusinessException("O profissional deve ter pelo menos um dia de disponibilidade configurado.");
        }

        // Validação interna dos horários
        for (DailyAvailability daily : newAvailability) {
            if (daily.isOpen() && !daily.startTime().isBefore(daily.endTime())) {
                throw new BusinessException(
                        "O horário de início deve ser anterior ao horário de término para: " + daily.dayOfWeek());
            }
        }

        this.availability = new ArrayList<>(newAvailability);
        this.updatedAt = LocalDateTime.now();
    }

    // --- REGRAS DE NEGÓCIO: SERVIÇOS ---

    public void validateCanPerform(List<Service> requestedServices) {
        boolean allSupported = requestedServices.stream()
                .allMatch(rs -> this.services.stream()
                        .anyMatch(ps -> ps.getId().equals(rs.getId())));

        if (!allSupported) {
            throw new BusinessException("Este profissional não realiza um ou mais dos serviços selecionados.");
        }
    }

    public void addService(Service service) {
        if (service == null)
            return;
        // Evita duplicatas baseada no ID
        if (this.services.stream().noneMatch(s -> s.getId().equals(service.getId()))) {
            this.services.add(service);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeService(UUID serviceId) {
        if (this.services.size() <= 1) {
            throw new BusinessException("O profissional não pode ficar sem nenhum serviço.");
        }
        this.services.removeIf(s -> s.getId().equals(serviceId));
        this.updatedAt = LocalDateTime.now();
    }

    // --- REGRAS DE NEGÓCIO: FINANCEIRO ---

    public void updateCommissionSettings(RemunerationType type, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O valor da remuneração não pode ser negativo.");
        }

        // Validação específica se for porcentagem
        if (type == RemunerationType.PERCENTAGE && value.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("A porcentagem não pode ser maior que 100.");
        }

        this.remunerationType = type;
        this.remunerationValue = value;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal calculateCommissionFor(BigDecimal finalPrice) {
        if (this.remunerationType == null || this.remunerationValue == null) {
            return BigDecimal.ZERO;
        }
        return this.remunerationType.calculate(finalPrice, this.remunerationValue);
    }

    public boolean hasConnectedAccount() {
        return this.gatewayAccountId != null && !this.gatewayAccountId.isBlank();
    }

    public void linkGatewayAccount(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new BusinessException("ID da conta do gateway inválido.");
        }
        this.gatewayAccountId = accountId;
        this.updatedAt = LocalDateTime.now();
    }

    // --- REGRAS DE NEGÓCIO: BLOQUEIO E AGENDA ---

    public void validateCanBlockTime(LocalDateTime start, LocalDateTime end, List<Appointment> existingAppointments) {
        if (!this.isActive)
            throw new BusinessException("Profissional inativo não pode realizar bloqueios.");

        boolean hasConflict = existingAppointments.stream()
                .anyMatch(app -> (app.getStatus() == AppointmentStatus.SCHEDULED
                        || app.getStatus() == AppointmentStatus.PENDING) &&
                // Lógica de intersecção de intervalos: (StartA < EndB) e (EndA > StartB)
                        app.getStartTime().isBefore(end) &&
                        app.getEndTime().isAfter(start));

        if (hasConflict) {
            throw new BusinessException("Não é possível bloquear: você já tem um cliente agendado nesse horário.");
        }
    }

    // --- ATUALIZAÇÃO DE PERFIL ---

    public void updateProfile(String name, String bio, String avatarUrl) {
        if (name != null && !name.isBlank())
            this.name = name;

        if (bio != null) {
            if (bio.length() > 500)
                throw new BusinessException("A bio não pode exceder 500 caracteres.");
            this.bio = bio;
        }

        if (avatarUrl != null)
            this.avatarUrl = avatarUrl;

        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSpecialties(List<String> newSpecialties) {
        if (newSpecialties != null) {
            this.specialties = new ArrayList<>(newSpecialties);
            this.updatedAt = LocalDateTime.now();
        }
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Professional that = (Professional) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}