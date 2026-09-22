package com.scrutinyai.controller;

import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.createReview(request, emailIfAuthenticated(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(reviewService.getReview(id, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @RequestParam(required = false) String language,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.getReviews(authentication.getName(), language, pageable));
    }

    @PostMapping("/{reviewId}/issues/{issueId}/explain")
    public ResponseEntity<ExplanationResponse> explainIssue(
            @PathVariable Long reviewId,
            @PathVariable Long issueId,
            Authentication authentication) {
        String explanation = reviewService.explainIssue(reviewId, issueId, authentication.getName());
        return ResponseEntity.ok(new ExplanationResponse(explanation));
    }

    @PostMapping("/{reviewId}/issues/{issueId}/fix")
    public ResponseEntity<AiFixResponse> fixIssue(
            @PathVariable Long reviewId,
            @PathVariable Long issueId,
            Authentication authentication) {
        AiFixResponse fix = reviewService.fixIssue(reviewId, issueId, authentication.getName());
        return ResponseEntity.ok(fix);
    }

    @PostMapping("/{reviewId}/generate-tests")
    public ResponseEntity<GeneratedTestResponse> generateTests(
            @PathVariable Long reviewId,
            Authentication authentication) {
        GeneratedTestResponse test = reviewService.generateTests(reviewId, authentication.getName());
        return ResponseEntity.ok(test);
    }

    private static String emailIfAuthenticated(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}