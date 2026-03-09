package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.review.CreateReviewRequest;
import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import com.stylo.api_agendamento.core.usecases.CreateReviewUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Avaliações", description = "Gestão de feedback e avaliações (1 a 5 estrelas) de agendamentos concluídos")
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    
    // Injetamos o repositório para buscas diretas de leitura (Query) sem precisar passar por UseCases complexos
    private final IReviewRepository reviewRepository;

    @Operation(summary = "Criar Avaliação", description = "Permite a um cliente avaliar um serviço/agendamento após a sua conclusão.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação criada e associada ao profissional com sucesso"),
            @ApiResponse(responseCode = "400", description = "Agendamento não concluído, nota inválida ou já avaliado anteriormente (Anti-Spam)"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (Tentativa de avaliar agendamento de terceiros)"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('appointment:write') or hasRole('CLIENT')")
    public ResponseEntity<Review> create(@RequestBody @Valid CreateReviewRequest request) {
        
        var input = new CreateReviewUseCase.Input(
            UUID.fromString(request.appointmentId()),
            request.rating(),
            request.comment()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createReviewUseCase.execute(input));
    }

    // ✨ NOVO ENDPOINT: Buscar avaliações de um Estabelecimento (Dashboard do Dono)
    @Operation(summary = "Listar Avaliações do Estabelecimento", description = "Retorna uma lista paginada de todas as avaliações feitas para qualquer profissional deste estabelecimento.")
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<PagedResult<Review>> listByProvider(
            @PathVariable UUID providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResult<Review> result = reviewRepository.findAllByProviderId(providerId, page, size);
        return ResponseEntity.ok(result);
    }

    // ✨ NOVO ENDPOINT: Buscar avaliações de um Profissional específico (Perfil Público)
    @Operation(summary = "Listar Avaliações do Profissional", description = "Retorna uma lista paginada de avaliações de um profissional específico.")
    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<PagedResult<Review>> listByProfessional(
            @PathVariable UUID professionalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResult<Review> result = reviewRepository.findAllByProfessionalId(professionalId, page, size);
        return ResponseEntity.ok(result);
    }
}