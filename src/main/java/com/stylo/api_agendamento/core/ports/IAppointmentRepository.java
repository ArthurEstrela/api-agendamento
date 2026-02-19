package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IAppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);

    /**
     * Busca agendamentos de um profissional em um dia específico.
     * Útil para montagem de grid/agenda visual.
     */
    List<Appointment> findAllByProfessionalIdAndDate(UUID professionalId, LocalDate date);

    /**
     * Verifica conflitos de horário (Double Booking).
     * Deve ignorar o próprio agendamento (caso seja uma edição).
     */
    boolean hasConflictingAppointment(UUID professionalId, LocalDateTime start, LocalDateTime end);

    /**
     * Busca agendamentos para relatórios administrativos do salão.
     */
    List<Appointment> findAllByProviderIdAndPeriod(UUID providerId, LocalDateTime start, LocalDateTime end);

    /**
     * Busca agendamentos que precisam de notificação (ex: lembrete 1h antes).
     */
    List<Appointment> findAppointmentsToNotify(LocalDateTime targetTime);

    /**
     * Busca receita gerada (Agendamentos Concluídos).
     */
    List<Appointment> findRevenueInPeriod(UUID providerId, LocalDateTime start, LocalDateTime end);

    /**
     * Calcula comissão total de um profissional no período.
     * Otimizado para não carregar objetos em memória, apenas o valor.
     */
    BigDecimal sumProfessionalCommissionByPeriod(UUID professionalId, LocalDateTime start, LocalDateTime end);

    /**
     * Histórico de agendamentos do cliente (Paginado).
     */
    PagedResult<Appointment> findAllByClientId(UUID clientId, int page, int size);

    /**
     * Busca agendamentos concluídos mas que ainda não tiveram comissão paga/processada.
     */
    List<Appointment> findPendingSettlementByProfessional(UUID professionalId);
}