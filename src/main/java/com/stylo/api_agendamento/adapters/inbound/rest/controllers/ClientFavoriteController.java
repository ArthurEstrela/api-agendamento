package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.context.SpringUserContext;
import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.usecases.GetClientFavoritesUseCase;
import com.stylo.api_agendamento.core.usecases.ManageFavoritesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients/me/favorites")
@RequiredArgsConstructor
@Tag(name = "Favoritos do Cliente", description = "Gerenciamento de estabelecimentos favoritos do cliente")
public class ClientFavoriteController {

    private final ManageFavoritesUseCase manageFavoritesUseCase;
    private final GetClientFavoritesUseCase getClientFavoritesUseCase;
    private final SpringUserContext userContext; // Ou o seu IUserContext injetado

    @PostMapping("/{providerId}")
    @Operation(summary = "Adiciona um estabelecimento aos favoritos")
    public ResponseEntity<Void> addFavorite(@PathVariable UUID providerId) {
        UUID clientId = userContext.getCurrentUserId();
        manageFavoritesUseCase.addFavorite(clientId, providerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{providerId}")
    @Operation(summary = "Remove um estabelecimento dos favoritos")
    public ResponseEntity<Void> removeFavorite(@PathVariable UUID providerId) {
        UUID clientId = userContext.getCurrentUserId();
        manageFavoritesUseCase.removeFavorite(clientId, providerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista os estabelecimentos favoritos do cliente")
    public ResponseEntity<PagedResult<ServiceProvider>> getFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        UUID clientId = userContext.getCurrentUserId();
        PagedResult<ServiceProvider> favorites = getClientFavoritesUseCase.execute(clientId, page, size);
        
        // Se desejar, faça o map para o seu DTO de Response (ex: ServiceProviderResponse)
        // return ResponseEntity.ok(favorites.map(ServiceProviderMapper::toResponse));
        return ResponseEntity.ok(favorites);
    }
}