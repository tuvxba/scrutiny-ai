package com.scrutinyai.repository;

import com.scrutinyai.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByUserEmail(String userEmail, Pageable pageable);

    Page<Review> findByUserEmailAndLanguage(String userEmail, String language, Pageable pageable);
}