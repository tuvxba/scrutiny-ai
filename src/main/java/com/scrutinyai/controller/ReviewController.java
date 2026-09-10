package com.scrutinyai.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scrutinyai.dto.AiFixResponse;
import com.scrutinyai.dto.ExplanationResponse;
import com.scrutinyai.dto.GeneratedTestResponse;
import com.scrutinyai.dto.ReviewRequest;
import com.scrutinyai.dto.ReviewResponse;
import com.scrutinyai.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.createReview(request, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReview(id));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews() {
        return ResponseEntity.ok(reviewService.getReviews());
    }

    @PostMapping("/{reviewId}/issues/{issueId}/explain")
    public ResponseEntity<ExplanationResponse> explainIssue(
            @PathVariable Long reviewId,
            @PathVariable Long issueId) {
        String explanation = reviewService.explainIssue(reviewId, issueId);
        return ResponseEntity.ok(new ExplanationResponse(explanation));
    }

    @PostMapping("/{reviewId}/issues/{issueId}/fix")
    public ResponseEntity<AiFixResponse> fixIssue(
            @PathVariable Long reviewId,
            @PathVariable Long issueId) {
        AiFixResponse fix = reviewService.fixIssue(reviewId, issueId);
        return ResponseEntity.ok(fix);
    }

    @PostMapping("/{reviewId}/generate-tests")
    public ResponseEntity<GeneratedTestResponse> generateTests(@PathVariable Long reviewId) {
        GeneratedTestResponse test = reviewService.generateTests(reviewId);
        return ResponseEntity.ok(test);
    }
}