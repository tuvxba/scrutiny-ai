package com.scrutinyai.ai;

public record AiIssueResult(
                String severity,
                Integer lineNumber,
                String title,
                String description,
                String suggestion) {
}