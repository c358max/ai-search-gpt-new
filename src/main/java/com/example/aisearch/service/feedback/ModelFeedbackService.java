package com.example.aisearch.service.feedback;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ModelFeedbackService {

    private final FeedbackAggregate overallAggregate = new FeedbackAggregate();
    private final Map<String, FeedbackAggregate> queryAggregates = new ConcurrentHashMap<>();

    public FeedbackSummary save(String query, int score) {
        validate(query, score);

        overallAggregate.add(score);
        FeedbackAggregate queryAggregate = queryAggregates.computeIfAbsent(query.trim(), key -> new FeedbackAggregate());
        queryAggregate.add(score);

        return new FeedbackSummary(query.trim(), queryAggregate.averageScore(), queryAggregate.ratingCount(), score);
    }

    public FeedbackSummary getByQuery(String query) {
        String normalizedQuery = normalizeQuery(query);
        FeedbackAggregate aggregate = queryAggregates.get(normalizedQuery);
        if (aggregate == null) {
            return new FeedbackSummary(normalizedQuery, 0.0, 0, 0);
        }
        return new FeedbackSummary(normalizedQuery, aggregate.averageScore(), aggregate.ratingCount(), 0);
    }

    public FeedbackOverallSummary getOverall() {
        return new FeedbackOverallSummary(overallAggregate.averageScore(), overallAggregate.ratingCount());
    }

    private void validate(String query, int score) {
        if (normalizeQuery(query).isBlank()) {
            throw new IllegalArgumentException("query 값이 비어 있습니다.");
        }
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("score 값은 1~5 사이여야 합니다.");
        }
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static final class FeedbackAggregate {
        private long totalScore;
        private long ratingCount;

        synchronized void add(int score) {
            totalScore += score;
            ratingCount += 1;
        }

        synchronized double averageScore() {
            if (ratingCount == 0) {
                return 0.0;
            }
            return (double) totalScore / ratingCount;
        }

        synchronized long ratingCount() {
            return ratingCount;
        }
    }
}
