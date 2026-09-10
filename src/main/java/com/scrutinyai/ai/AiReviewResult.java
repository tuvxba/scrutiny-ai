package com.scrutinyai.ai;

import java.util.List;

public record AiReviewResult(
    int score, 
    String summary, 
    List<AiIssueResult> issues
) {}