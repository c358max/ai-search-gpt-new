package com.example.aisearch.service.feedback;

public record FeedbackSummary(
        String query,
        double averageScore,
        long ratingCount,
        int savedScore
) {
}
