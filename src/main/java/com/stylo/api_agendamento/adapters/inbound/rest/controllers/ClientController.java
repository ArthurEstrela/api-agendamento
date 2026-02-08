package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.ports.IClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final IClientRepository clientRepository;

    @GetMapping("/{id}")
    public ResponseEntity<Client> getById(@PathVariable String id) {
        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Client> updateProfile(
            @PathVariable String id, 
            @RequestBody Client updatedData) {
        
        return clientRepository.findById(id)
                .map(client -> {
                    // Lógica de atualização aqui
                    return ResponseEntity.ok(clientRepository.save(client));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}