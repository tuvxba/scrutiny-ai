package com.scrutinyai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scrutinyai.entity.GeneratedTest;

public interface GeneratedTestRepository extends JpaRepository<GeneratedTest, Long> {
}