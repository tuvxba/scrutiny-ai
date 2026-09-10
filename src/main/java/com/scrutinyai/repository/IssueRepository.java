package com.scrutinyai.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scrutinyai.entity.Issue;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByReviewId(Long reviewId);
}