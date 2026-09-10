package com.scrutinyai.dto;

import com.scrutinyai.entity.AiFix;

public record AiFixResponse(
        Long id,
        String originalSnippet,
        String fixedSnippet,
        boolean applied
) {
    public static AiFixResponse from(AiFix aiFix) {
        return new AiFixResponse(
                aiFix.getId(),
                aiFix.getOriginalSnippet(),
                aiFix.getFixedSnippet(),
                aiFix.isApplied()
        );
    }
}