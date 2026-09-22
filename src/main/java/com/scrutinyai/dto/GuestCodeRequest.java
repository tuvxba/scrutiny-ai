package com.scrutinyai.dto;

import jakarta.validation.constraints.NotBlank;

public record GuestCodeRequest(
                @NotBlank String language,
                @NotBlank String codeSnippet) {
}
