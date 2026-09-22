package com.scrutinyai.dto;

import jakarta.validation.constraints.NotBlank;

public record GuestIssueRequest(
                @NotBlank String language,
                @NotBlank String codeSnippet,
                @NotBlank String severity,
                Integer lineNumber,
                @NotBlank String title,
                @NotBlank String description,
                String suggestion) {
}
