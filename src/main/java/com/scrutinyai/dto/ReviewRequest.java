package com.scrutinyai.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(
        @NotBlank(message = "Dil boş olamaz") String language,
        @NotBlank(message = "Kod boş olamaz") String codeSnippet
) {}