package com.scrutinyai.dto;

import com.scrutinyai.entity.Issue;

public record IssueResponse(
        Long id,
        String severity,
        Integer lineNumber,
        String title,
        String description,
        String suggestion) {
    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getId(),
                issue.getSeverity().name(),
                issue.getLineNumber(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getSuggestion());
    }
}