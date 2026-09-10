package com.scrutinyai.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scrutinyai.entity.AiFix;

public interface AiFixRepository extends JpaRepository<AiFix, Long> {
    Optional<AiFix> findByIssueId(Long issueId);
}