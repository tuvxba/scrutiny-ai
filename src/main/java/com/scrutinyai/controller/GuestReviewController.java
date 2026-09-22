package com.scrutinyai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scrutinyai.dto.AiFixResponse;
import com.scrutinyai.dto.ExplanationResponse;
import com.scrutinyai.dto.GeneratedTestResponse;
import com.scrutinyai.dto.GuestCodeRequest;
import com.scrutinyai.dto.GuestIssueRequest;
import com.scrutinyai.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestReviewController {

    private final ReviewService reviewService;

    @PostMapping("/explain")
    public ResponseEntity<ExplanationResponse> explain(@Valid @RequestBody GuestIssueRequest request) {
        return ResponseEntity.ok(new ExplanationResponse(reviewService.explainGuestIssue(request)));
    }

    @PostMapping("/fix")
    public ResponseEntity<AiFixResponse> fix(@Valid @RequestBody GuestIssueRequest request) {
        return ResponseEntity.ok(reviewService.fixGuestIssue(request));
    }

    @PostMapping("/generate-tests")
    public ResponseEntity<GeneratedTestResponse> generateTests(@Valid @RequestBody GuestCodeRequest request) {
        return ResponseEntity.ok(reviewService.generateGuestTests(request));
    }
}
