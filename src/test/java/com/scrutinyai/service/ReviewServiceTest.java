package com.scrutinyai.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scrutinyai.ai.AiCodeReviewService;
import com.scrutinyai.ai.AiIssueResult;
import com.scrutinyai.ai.AiReviewResult;
import com.scrutinyai.dto.ReviewRequest;
import com.scrutinyai.dto.ReviewResponse;
import com.scrutinyai.entity.Review;
import com.scrutinyai.entity.User;
import com.scrutinyai.enums.ReviewStatus;
import com.scrutinyai.exception.ResourceNotFoundException;
import com.scrutinyai.kafka.ReviewEventProducer;
import com.scrutinyai.repository.AiFixRepository;
import com.scrutinyai.repository.GeneratedTestRepository;
import com.scrutinyai.repository.IssueRepository;
import com.scrutinyai.repository.ReviewRepository;
import com.scrutinyai.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiCodeReviewService aiCodeReviewService;

    @Mock
    private AiFixRepository aiFixRepository;

    @Mock
    private GeneratedTestRepository generatedTestRepository;

    @Mock
    private ReviewEventProducer reviewEventProducer;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void getReview_userCanAccessOwnReview() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        Review review = Review.builder()
                .id(1L)
                .language("java")
                .codeSnippet("public void test() {}")
                .status(ReviewStatus.COMPLETED)
                .score(80)
                .aiSummary("Good code")
                .user(user)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(issueRepository.findByReviewId(1L)).thenReturn(List.of());

        ReviewResponse result = reviewService.getReview(1L, "test@example.com");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.language()).isEqualTo("java");
    }

    @Test
    void getReview_throwsWhenReviewNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getReview(99L, "test@example.com"));
    }

    @Test
    void getReview_throwsWhenAccessingOthersReview() {
        User owner = User.builder()
                .id(1L)
                .email("owner@example.com")
                .build();

        Review review = Review.builder()
                .id(1L)
                .language("java")
                .status(ReviewStatus.COMPLETED)
                .user(owner)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getReview(1L, "other@example.com"));
    }

    @Test
    void createReview_savesAsPendingAndPublishesEvent() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        ReviewRequest request = new ReviewRequest("java", "public void test() {}");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ReviewResponse result = reviewService.createReview(request, "test@example.com");

        assertThat(result.status()).isEqualTo("PENDING");
        verify(reviewEventProducer).publishReviewRequested(1L);
        verifyNoInteractions(aiCodeReviewService);
    }

    @Test
    void createReview_guestRunsAiSynchronouslyWithoutPersisting() {
        ReviewRequest request = new ReviewRequest("java", "public void test() {}");
        when(aiCodeReviewService.reviewCode("java", "public void test() {}"))
                .thenReturn(new AiReviewResult(72, "Off-by-one risk", List.of(
                        new AiIssueResult("HIGH", 4, "Off-by-one", "Loop bound is inclusive", "Use < size"))));

        ReviewResponse result = reviewService.createReview(request, null);

        assertThat(result.id()).isNull();
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.score()).isEqualTo(72);
        assertThat(result.issues()).hasSize(1);
        assertThat(result.issues().get(0).id()).isNull();
        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(reviewEventProducer, userRepository);
    }
}
