package com.stylo.api_agendamento.adapters.inbound.rest.exceptions;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException;

import lombok.extern.slf4j.Slf4j; // ✨ Importante para o log
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j // ✨ Anotação do Lombok para injetar o 'log'
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Seus Handlers Específicos (Já existiam) ---

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Recurso não encontrado");
        problemDetail.setType(URI.create("https://stylo.com/errors/not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ResponseEntity<ProblemDetail> handleScheduleConflict(ScheduleConflictException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Conflito de Agendamento");
        problemDetail.setType(URI.create("https://stylo.com/errors/conflict"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Regra de Negócio Violada");
        problemDetail.setType(URI.create("https://stylo.com/errors/business-rule"));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erro na validação dos campos.");
        problemDetail.setTitle("Dados Inválidos");
        problemDetail.setProperty("fields", errors);
        
        return ResponseEntity.badRequest().body(problemDetail);
    }

    // --- ✨ O NOVO CATCH-ALL HANDLER (A "Rede de Segurança") ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUncaughtException(Exception ex) {
        // 1. LOG DO ERRO REAL (Crucial para você!)
        // O log.error grava o Stack Trace completo no console/arquivo do servidor.
        // Assim você descobre se foi NullPointer, Banco fora do ar, etc.
        log.error("ERRO CRÍTICO NÃO TRATADO: ", ex);

        // 2. MENSAGEM GENÉRICA PARA O USUÁRIO (Segurança)
        // Ocultamos a causa real para não expor vulnerabilidades.
        String genericMessage = "Ocorreu um erro interno inesperado. Por favor, tente novamente mais tarde.";

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, genericMessage);
        problemDetail.setTitle("Erro Interno do Servidor");
        problemDetail.setType(URI.create("https://stylo.com/errors/internal-server-error"));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}