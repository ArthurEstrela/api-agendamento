package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.UpdateProfilePictureUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Utilizadores", description = "Gestão de perfil e imagem de avatar")
public class ProfileImageController {

    private final UpdateProfilePictureUseCase updateProfilePictureUseCase;

    @Operation(summary = "Upload de foto de perfil", description = "Envia uma imagem (JPEG/PNG), guarda no Storage (ex: Firebase/S3) e atualiza a hiperligação no perfil do utilizador logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem de perfil atualizada com sucesso. Retorna o URL público."),
            @ApiResponse(responseCode = "400", description = "Ficheiro inválido, vazio ou formato não suportado."),
            @ApiResponse(responseCode = "401", description = "Acesso não autorizado (Token ausente ou inválido)")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()") // Segurança: Apenas utilizadores com sessão iniciada podem alterar o avatar
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // 1. Validações básicas de segurança (Proteção do Storage)
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "O ficheiro enviado está vazio."));
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Apenas ficheiros de imagem (ex: JPG, PNG) são permitidos."));
        }

        // 2. ✨ CORREÇÃO: Passagem dos 4 parâmetros exatos exigidos pelo UseCase (incluindo o getSize())
        String url = updateProfilePictureUseCase.execute(
                file.getInputStream(),
                contentType,
                file.getOriginalFilename(),
                file.getSize() 
        );

        return ResponseEntity.ok(Map.of("url", url));
    }
}