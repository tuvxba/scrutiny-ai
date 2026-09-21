package com.scrutinyai.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(
                @NotBlank(message = "Language is required") String language,
                @NotBlank(message = "Code snippet is required") String codeSnippet) {
}