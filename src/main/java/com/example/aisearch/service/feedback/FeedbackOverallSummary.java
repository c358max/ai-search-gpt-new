package com.example.aisearch.service.feedback;

public record FeedbackOverallSummary(
        double averageScore,
        long ratingCount
) {
}
