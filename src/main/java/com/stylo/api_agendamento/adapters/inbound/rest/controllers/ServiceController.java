package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.CreateServiceRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.UpdateServiceRequest;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.usecases.CreateServiceUseCase;
import com.stylo.api_agendamento.core.usecases.UpdateServiceUseCase;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final CreateServiceUseCase createServiceUseCase;
    private final IServiceRepository serviceRepository;
    private final UpdateServiceUseCase updateServiceUseCase;

    @PostMapping
    public ResponseEntity<Service> create(@RequestBody @Valid CreateServiceRequest request) {
        // Alinhado com o record de 5 parâmetros do UseCase
        var input = new CreateServiceUseCase.CreateServiceInput(
                request.name(),
                request.description(),
                request.duration(), // Certifique-se que o record CreateServiceRequest use 'duration'
                request.price(),
                request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createServiceUseCase.execute(input));
    }

    @GetMapping
    public ResponseEntity<List<Service>> listAll() {
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Service>> listByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(serviceRepository.findByCategoryId(categoryId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<com.stylo.api_agendamento.core.domain.Service> update(
            @PathVariable String id,
            @RequestBody @Valid UpdateServiceRequest request) {

        var input = new UpdateServiceUseCase.UpdateServiceInput(
                id,
                request.name(),
                request.description(),
                request.duration(),
                request.price());

        return ResponseEntity.ok(updateServiceUseCase.execute(input));
    }

}