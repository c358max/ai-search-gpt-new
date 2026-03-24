package com.example.aisearch.controller.dto;

import com.example.aisearch.service.feedback.FeedbackOverallSummary;

public record ModelFeedbackSummaryResponseDto(
        double averageScore,
        long ratingCount
) {
    public static ModelFeedbackSummaryResponseDto from(FeedbackOverallSummary summary) {
        return new ModelFeedbackSummaryResponseDto(summary.averageScore(), summary.ratingCount());
    }
}
