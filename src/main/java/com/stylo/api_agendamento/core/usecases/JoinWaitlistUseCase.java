package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.Waitlist;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.ports.IWaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class JoinWaitlistUseCase {

    private final IWaitlistRepository waitlistRepository;
    private final IUserRepository userRepository;
    private final IProfessionalRepository professionalRepository;

    @Transactional
    public void execute(Input input) {
        // 1. Busca e Valida o Cliente
        User client = userRepository.findById(input.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado."));

        // 2. Busca o Profissional para garantir o vínculo com o Estabelecimento (Tenant)
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 3. Criação via Factory de Domínio (já valida se a data é futura)
        Waitlist entry = Waitlist.create(
                professional.getId(),
                professional.getServiceProviderId(),
                client.getName(),
                new ClientPhone(client.getPhoneNumber()),
                client.getEmail(),
                input.desiredDate()
        );

        // 4. Vincula o ID do cliente cadastrado
        entry.linkClient(client.getId());

        waitlistRepository.save(entry);
        
        log.info("Cliente {} entrou na lista de espera para o profissional {} na data {}.", 
                client.getName(), professional.getName(), input.desiredDate());
    }

    public record Input(UUID clientId, UUID professionalId, LocalDate desiredDate) {}
}