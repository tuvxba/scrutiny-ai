package com.scrutinyai.dto;

import java.util.List;

import com.scrutinyai.entity.Review;

public record ReviewResponse(
        Long id,
        String language,
        String status,
        Integer score,
        String aiSummary,
        String codeSnippet,
        List<IssueResponse> issues) {
    public static ReviewResponse summary(Review review) {
        return new ReviewResponse(review.getId(), review.getLanguage(), review.getStatus().name(),
                review.getScore(), review.getAiSummary(), null, null);
    }

    public static ReviewResponse detail(Review review, List<IssueResponse> issues) {
        return new ReviewResponse(review.getId(), review.getLanguage(), review.getStatus().name(),
                review.getScore(), review.getAiSummary(), review.getCodeSnippet(), issues);
    }
}