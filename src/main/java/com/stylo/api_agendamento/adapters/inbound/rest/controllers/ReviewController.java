package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.review.CreateReviewRequest;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.usecases.CreateReviewUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;

    @PostMapping
    public ResponseEntity<Review> create(@RequestBody @Valid CreateReviewRequest request) {
        var input = new CreateReviewUseCase.CreateReviewInput(
            request.appointmentId(),
            request.rating(),
            request.comment()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createReviewUseCase.execute(input));
    }
}