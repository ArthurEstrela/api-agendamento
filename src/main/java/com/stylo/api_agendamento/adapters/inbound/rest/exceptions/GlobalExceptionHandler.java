package com.stylo.api_agendamento.adapters.inbound.rest.exceptions;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode; // ✨ NOVA IMPORTAÇÃO
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex) {
        // ✨ ATUALIZADO: Usando HttpStatusCode.valueOf(404)
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), ex.getMessage());
        problemDetail.setTitle("Recurso não encontrado");
        problemDetail.setType(URI.create("https://stylo.com/errors/not-found"));
        return ResponseEntity.status(404).body(problemDetail);
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ResponseEntity<ProblemDetail> handleScheduleConflict(ScheduleConflictException ex) {
        // ✨ ATUALIZADO: Usando HttpStatusCode.valueOf(409)
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(409), ex.getMessage());
        problemDetail.setTitle("Conflito de Agendamento");
        problemDetail.setType(URI.create("https://stylo.com/errors/conflict"));
        return ResponseEntity.status(409).body(problemDetail);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        // ✨ CORREÇÃO DO AVISO: Usando HttpStatusCode.valueOf(422) ao invés do Enum
        // UNPROCESSABLE_ENTITY
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), ex.getMessage());
        problemDetail.setTitle("Regra de Negócio Violada");
        problemDetail.setType(URI.create("https://stylo.com/errors/business-rule"));
        return ResponseEntity.status(422).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // ✨ ATUALIZADO: Usando HttpStatusCode.valueOf(400)
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                "Erro na validação dos campos.");
        problemDetail.setTitle("Dados Inválidos");
        problemDetail.setProperty("fields", errors);

        return ResponseEntity.status(400).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUncaughtException(Exception ex) {
        // 1. LOG DO ERRO REAL
        log.error("ERRO CRÍTICO NÃO TRATADO: ", ex);

        // 2. MENSAGEM GENÉRICA PARA O USUÁRIO (Segurança)
        String genericMessage = "Ocorreu um erro interno inesperado. Por favor, tente novamente mais tarde.";

        // ✨ ATUALIZADO: Usando HttpStatusCode.valueOf(500)
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500), genericMessage);
        problemDetail.setTitle("Erro Interno do Servidor");
        problemDetail.setType(URI.create("https://stylo.com/errors/internal-server-error"));

        return ResponseEntity.status(500).body(problemDetail);
    }
}