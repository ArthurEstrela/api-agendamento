package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.*;
import com.stylo.api_agendamento.core.usecases.GetProfessionalProfileUseCase;
import com.stylo.api_agendamento.core.usecases.UpdateProfessionalAvailabilityUseCase;
import com.stylo.api_agendamento.core.usecases.UpdateProfessionalCommissionUseCase;
import com.stylo.api_agendamento.core.usecases.BlockProfessionalTimeUseCase;
import com.stylo.api_agendamento.core.usecases.dto.ProfessionalProfile;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.stylo.api_agendamento.core.domain.User;

import java.util.List;

@RestController
@RequestMapping("/v1/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

        private final GetProfessionalProfileUseCase getProfessionalProfileUseCase;
        private final UpdateProfessionalAvailabilityUseCase updateAvailabilityUseCase;
        private final BlockProfessionalTimeUseCase blockTimeUseCase;
        private final UpdateProfessionalCommissionUseCase updateCommissionUseCase;

        @GetMapping("/{id}/profile")
        public ResponseEntity<ProfessionalProfile> getProfile(@PathVariable String id) {
                return ResponseEntity.ok(getProfessionalProfileUseCase.execute(id));
        }

        @PutMapping("/{id}/availability")
        public ResponseEntity<Void> updateAvailability(
                        @PathVariable String id,
                        @RequestBody @Valid UpdateAvailabilityRequest request) {

                // Como DailyAvailability é um record, usamos o construtor padrão
                // Mapeamos os campos do DTO para os campos do record de domínio
                List<DailyAvailability> availabilities = request.availabilities().stream()
                                .map(req -> new DailyAvailability(
                                                req.dayOfWeek(),
                                                req.isWorkingDay(), // Certifique-se que o DTO tem esse nome ou mude
                                                                    // para req.isOpen()
                                                req.startTime(),
                                                req.endTime()))
                                .toList();

                var input = new UpdateProfessionalAvailabilityUseCase.UpdateAvailabilityInput(
                                id,
                                availabilities);

                updateAvailabilityUseCase.execute(input);
                return ResponseEntity.noContent().build();
        }

        @PostMapping("/{id}/block")
        public ResponseEntity<Void> blockTime(
                        @PathVariable String id,
                        @RequestBody @Valid BlockProfessionalTimeRequest request) {

                var input = new BlockProfessionalTimeUseCase.BlockTimeInput(
                                id,
                                request.startTime(),
                                request.endTime(),
                                request.reason());

                blockTimeUseCase.execute(input);
                return ResponseEntity.noContent().build();
        }

        @PatchMapping("/{id}/commission")
        public ResponseEntity<Void> updateCommission(
                        @PathVariable String id,
                        @AuthenticationPrincipal User user, // ✨ Pega o usuário logado do Spring Security
                        @RequestBody @Valid UpdateCommissionRequest request) {

                var input = new UpdateProfessionalCommissionUseCase.UpdateCommissionInput(
                                user.getId(), // O ID do usuário logado é o requester
                                id, // O ID do path é o alvo da alteração
                                request.type(),
                                request.value());

                updateCommissionUseCase.execute(input);
                return ResponseEntity.noContent().build();
        }
}