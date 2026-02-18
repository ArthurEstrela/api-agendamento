package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.UpdateProfilePictureUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/v1/users/profile-picture")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gestão de perfil e imagem")
public class ProfileImageController {

    private final UpdateProfilePictureUseCase updateProfilePictureUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto de perfil", description = "Envia uma imagem, salva no Storage e atualiza o perfil do usuário.")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // Validações básicas
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (!file.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Apenas imagens são permitidas."));
        }

        String url = updateProfilePictureUseCase.execute(
                file.getInputStream(),
                file.getContentType(),
                file.getOriginalFilename()
        );

        return ResponseEntity.ok(Map.of("url", url));
    }
}