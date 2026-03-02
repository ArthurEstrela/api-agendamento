package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
            List<Service> services, List<DailyAvailability> availability, boolean isOwner) {

        if (name == null || name.isBlank())
            throw new BusinessException("Nome do profissional é obrigatório.");
        if (email == null || !email.contains("@"))
            throw new BusinessException("E-mail inválido.");
        if (providerId == null)
            throw new BusinessException("O profissional deve estar vinculado a um estabelecimento.");

        return Professional.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .serviceProviderId(providerId)
                .services(new ArrayList<>(services))
                .availability(new ArrayList<>(availability))
                .slotInterval(30)
                .isOwner(isOwner)
                .isActive(true)
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

    public List<LocalTime> calculateAvailableSlots(
            LocalDate date,
            int totalDurationMinutes,
            List<Appointment> existingAppointments,
            ZoneId providerZoneId) {

        // 1. Obter configuração do dia (Expediente)
        DailyAvailability dailyConfig = this.availability.stream()
                .filter(a -> a.dayOfWeek() == date.getDayOfWeek() && a.isOpen())
                .findFirst()
                .orElse(null);

        // Se não atende no dia ou não tem configuração, retorna lista vazia
        if (dailyConfig == null) {
            return new ArrayList<>();
        }

        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime currentSlot = dailyConfig.startTime();

        // Ajuste de fuso horário para "hoje"
        LocalTime nowInProviderZone = LocalTime.now(providerZoneId);
        LocalDate todayInProviderZone = LocalDate.now(providerZoneId);

        // O último horário possível deve comportar a duração total dos serviços
        LocalTime lastPossibleSlot = dailyConfig.endTime().minusMinutes(totalDurationMinutes);

        while (!currentSlot.isAfter(lastPossibleSlot)) {
            LocalTime slotStart = currentSlot;
            LocalTime slotEnd = currentSlot.plusMinutes(totalDurationMinutes);

            // Regra 1: Não permitir horários que já passaram hoje
            boolean isPast = date.isEqual(todayInProviderZone) && slotStart.isBefore(nowInProviderZone);

            // Regra 2: Verificar sobreposição com qualquer ocupação existente
            boolean hasConflict = existingAppointments.stream().anyMatch(occ -> {
                LocalTime occStart = occ.getStartTime().toLocalTime();
                LocalTime occEnd = occ.getEndTime().toLocalTime();
                // (StartA < EndB) AND (EndA > StartB) -> Sobreposição detectada
                return slotStart.isBefore(occEnd) && slotEnd.isAfter(occStart);
            });

            if (!hasConflict && !isPast) {
                availableSlots.add(slotStart);
            }

            // Incrementa o slot conforme o intervalo configurado no perfil do profissional
            currentSlot = currentSlot.plusMinutes(this.slotInterval);
        }

        return availableSlots;
    }

    // --- REGRAS DE NEGÓCIO: ALINHAMENTO DE SLOTS ---

    public void validateSlotAlignment(LocalDateTime requestedStartTime) {
        DailyAvailability dailyConfig = this.availability.stream()
                .filter(a -> a.dayOfWeek() == requestedStartTime.getDayOfWeek() && a.isOpen())
                .findFirst()
                .orElseThrow(() -> new BusinessException("O profissional não atende neste dia da semana."));

        // Calcula a diferença em minutos desde a hora que o profissional começa a
        // trabalhar
        long minutesFromStart = java.time.Duration.between(dailyConfig.startTime(), requestedStartTime.toLocalTime())
                .toMinutes();

        // Se o resto da divisão não for zero, o horário está "quebrado"
        if (minutesFromStart % this.slotInterval != 0) {
            throw new BusinessException(String.format(
                    "O horário solicitado (%s) é inválido. Os agendamentos devem seguir intervalos exatos de %d minutos a partir do início do expediente (%s).",
                    requestedStartTime.toLocalTime().toString(),
                    this.slotInterval,
                    dailyConfig.startTime().toString()));
        }
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

    // --- REGRAS DE NEGÓCIO: BLOQUEIO E AGENDA ---

    public void validateCanBlockTime(LocalDateTime start, LocalDateTime end, List<Appointment> existingAppointments) {
        if (!this.isActive)
            throw new BusinessException("Profissional inativo não pode realizar bloqueios.");

        boolean hasConflict = existingAppointments.stream()
                .anyMatch(app -> (app.getStatus() == AppointmentStatus.SCHEDULED ||
                        app.getStatus() == AppointmentStatus.PENDING ||
                        app.getStatus() == AppointmentStatus.BLOCKED // ✨ ADICIONADO: Verifica bloqueios existentes
                ) &&
                // Lógica de intersecção de intervalos: (StartA < EndB) e (EndA > StartB)
                        app.getStartTime().isBefore(end) &&
                        app.getEndTime().isAfter(start));

        if (hasConflict) {
            throw new BusinessException(
                    "Não é possível bloquear: já existe um cliente agendado ou um bloqueio neste horário.");
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

    public void updateServices(List<Service> newServices) {
        if (newServices == null || newServices.isEmpty()) {
            throw new BusinessException("O profissional deve realizar pelo menos um serviço.");
        }
        this.services.clear();
        this.services.addAll(newServices);
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