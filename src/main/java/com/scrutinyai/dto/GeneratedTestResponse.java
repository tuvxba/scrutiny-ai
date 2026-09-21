package com.scrutinyai.dto;

import com.scrutinyai.entity.GeneratedTest;

public record GeneratedTestResponse(
        Long id,
        String testCode) {
    public static GeneratedTestResponse from(GeneratedTest generatedTest) {
        return new GeneratedTestResponse(
                generatedTest.getId(),
                generatedTest.getTestCode());
    }
}