package com.scrutinyai.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewEventProducer {

    private static final String TOPIC = "code-review-requested";

    private final KafkaTemplate<String, ReviewRequestedEvent> kafkaTemplate;

    public void publishReviewRequested(Long reviewId) {
        kafkaTemplate.send(TOPIC, new ReviewRequestedEvent(reviewId));
    }
}