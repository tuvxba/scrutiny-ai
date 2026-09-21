package com.scrutinyai.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.scrutinyai.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final ReviewService reviewService;

    @KafkaListener(topics = "code-review-requested", groupId = "scrutiny-ai-review-group")
    public void handleReviewRequested(ReviewRequestedEvent event) {
        log.info("Review is being processed: reviewId={}", event.reviewId());

        try {
            reviewService.processReview(event.reviewId());
        } catch (Exception e) {
            log.error("An error occurred while processing the review: reviewId={}", event.reviewId(), e);
        }
    }
}