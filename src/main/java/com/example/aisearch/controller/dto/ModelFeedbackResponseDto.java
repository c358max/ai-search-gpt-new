package com.example.aisearch.controller.dto;

import com.example.aisearch.service.feedback.FeedbackSummary;

public record ModelFeedbackResponseDto(
        String query,
        double averageScore,
        long ratingCount,
        int savedScore
) {
    public static ModelFeedbackResponseDto from(FeedbackSummary summary) {
        return new ModelFeedbackResponseDto(
                summary.query(),
                summary.averageScore(),
                summary.ratingCount(),
                summary.savedScore()
        );
    }
}
