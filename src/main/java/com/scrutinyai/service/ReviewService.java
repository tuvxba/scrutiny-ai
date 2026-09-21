package com.scrutinyai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scrutinyai.ai.AiCodeReviewService;
import com.scrutinyai.ai.AiReviewResult;
import com.scrutinyai.dto.AiFixResponse;
import com.scrutinyai.dto.GeneratedTestResponse;
import com.scrutinyai.dto.IssueResponse;
import com.scrutinyai.dto.ReviewRequest;
import com.scrutinyai.dto.ReviewResponse;
import com.scrutinyai.entity.AiFix;
import com.scrutinyai.entity.GeneratedTest;
import com.scrutinyai.entity.Issue;
import com.scrutinyai.entity.Review;
import com.scrutinyai.entity.User;
import com.scrutinyai.enums.IssueSeverity;
import com.scrutinyai.enums.ReviewStatus;
import com.scrutinyai.exception.ResourceNotFoundException;
import com.scrutinyai.kafka.ReviewEventProducer;
import com.scrutinyai.repository.AiFixRepository;
import com.scrutinyai.repository.GeneratedTestRepository;
import com.scrutinyai.repository.IssueRepository;
import com.scrutinyai.repository.ReviewRepository;
import com.scrutinyai.repository.UserRepository;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ReviewService {

        private final ReviewRepository reviewRepository;
        private final IssueRepository issueRepository;
        private final UserRepository userRepository;
        private final AiCodeReviewService aiCodeReviewService;
        private final AiFixRepository aiFixRepository;
        private final GeneratedTestRepository generatedTestRepository;
        private final ReviewEventProducer reviewEventProducer;

        public ReviewResponse createReview(ReviewRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new IllegalStateException("User not found: " + userEmail));

                Review review = Review.builder()
                                .language(request.language())
                                .codeSnippet(request.codeSnippet())
                                .status(ReviewStatus.PENDING)
                                .user(user)
                                .build();
                review = reviewRepository.save(review);

                reviewEventProducer.publishReviewRequested(review.getId());

                return ReviewResponse.detail(review, List.of());
        }

        @Transactional(readOnly = true)
        public ReviewResponse getReview(Long id, String userEmail) {
                Review review = reviewRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));

                if (!review.getUser().getEmail().equals(userEmail)) {
                        throw new ResourceNotFoundException("Review not found: " + id);
                }

                List<IssueResponse> issues = issueRepository.findByReviewId(id).stream()
                                .map(IssueResponse::from)
                                .toList();

                return ReviewResponse.detail(review, issues);
        }

        @Transactional(readOnly = true)
        public Page<ReviewResponse> getReviews(String userEmail, String language, Pageable pageable) {
                Page<Review> reviews = (language != null)
                                ? reviewRepository.findByUserEmailAndLanguage(userEmail, language, pageable)
                                : reviewRepository.findByUserEmail(userEmail, pageable);

                return reviews.map(ReviewResponse::summary);
        }

        @Transactional
        public String explainIssue(Long reviewId, Long issueId, String userEmail) {
                Review review = reviewRepository.findById(reviewId)
                                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

                if (!review.getUser().getEmail().equals(userEmail)) {
                        throw new ResourceNotFoundException("Review not found: " + reviewId);
                }

                Issue issue = issueRepository.findById(issueId)
                                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

                if (!issue.getReview().getId().equals(review.getId())) {
                        throw new ResourceNotFoundException("This issue does not belong to this review");
                }

                if (issue.getExplanation() != null) {
                        return issue.getExplanation();
                }

                String explanation = aiCodeReviewService.explainIssue(review, issue);
                issue.setExplanation(explanation);
                issueRepository.save(issue);

                return explanation;
        }

        @Transactional
        public AiFixResponse fixIssue(Long reviewId, Long issueId, String userEmail) {
                Review review = reviewRepository.findById(reviewId)
                                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

                if (!review.getUser().getEmail().equals(userEmail)) {
                        throw new ResourceNotFoundException("Review not found: " + reviewId);
                }

                Issue issue = issueRepository.findById(issueId)
                                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

                if (!issue.getReview().getId().equals(review.getId())) {
                        throw new ResourceNotFoundException("This issue does not belong to this review");
                }

                AiFix aiFix = aiFixRepository.findByIssueId(issueId)
                                .orElseGet(() -> {
                                        String fixedCode = aiCodeReviewService.fixIssue(review, issue);

                                        AiFix newFix = AiFix.builder()
                                                        .originalSnippet(review.getCodeSnippet())
                                                        .fixedSnippet(fixedCode)
                                                        .issue(issue)
                                                        .build();

                                        return aiFixRepository.save(newFix);
                                });

                return AiFixResponse.from(aiFix);
        }

        @Transactional
        public GeneratedTestResponse generateTests(Long reviewId, String userEmail) {
                Review review = reviewRepository.findById(reviewId)
                                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

                if (!review.getUser().getEmail().equals(userEmail)) {
                        throw new ResourceNotFoundException("Review not found: " + reviewId);
                }

                String testCode = aiCodeReviewService.generateTests(review);

                GeneratedTest generatedTest = GeneratedTest.builder()
                                .testCode(testCode)
                                .review(review)
                                .build();

                GeneratedTest saved = generatedTestRepository.save(generatedTest);
                return GeneratedTestResponse.from(saved);
        }

        public void processReview(Long reviewId) {
                Review review = reviewRepository.findById(reviewId)
                                .orElseThrow(() -> new IllegalStateException("Review not found: " + reviewId));

                review.setStatus(ReviewStatus.PROCESSING);
                reviewRepository.save(review);

                try {
                        AiReviewResult aiResult = aiCodeReviewService.reviewCode(review.getLanguage(),
                                        review.getCodeSnippet());

                        review.setScore(aiResult.score());
                        review.setAiSummary(aiResult.summary());
                        review.setStatus(ReviewStatus.COMPLETED);
                        reviewRepository.save(review);

                        Review savedReview = review;
                        List<Issue> issues = aiResult.issues().stream()
                                        .map(i -> Issue.builder()
                                                        .severity(IssueSeverity.valueOf(i.severity()))
                                                        .lineNumber(i.lineNumber())
                                                        .title(i.title())
                                                        .description(i.description())
                                                        .suggestion(i.suggestion())
                                                        .review(savedReview)
                                                        .build())
                                        .toList();
                        issueRepository.saveAll(issues);

                } catch (Exception e) {
                        review.setStatus(ReviewStatus.FAILED);
                        reviewRepository.save(review);
                }
        }

}