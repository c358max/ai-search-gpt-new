package com.example.aisearch.service.feedback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ModelFeedbackService {

    private final ModelFeedbackRepository modelFeedbackRepository;
    private final String modelName;

    public ModelFeedbackService(
            ModelFeedbackRepository modelFeedbackRepository,
            @Value("${ai-search.model-key:default}") String modelName
    ) {
        this.modelFeedbackRepository = modelFeedbackRepository;
        this.modelName = modelName;
    }

    public FeedbackSummary save(String query, int score, String sortOption, Long searchDurationMillis) {
        validate(query, score);
        String normalizedQuery = normalizeQuery(query);
        String normalizedSortOption = normalizeSortOption(sortOption);
        long normalizedSearchDurationMillis = normalizeSearchDurationMillis(searchDurationMillis);

        modelFeedbackRepository.save(modelName, normalizedQuery, score, normalizedSortOption, normalizedSearchDurationMillis);
        FeedbackSummary summary = modelFeedbackRepository.getByQuery(modelName, normalizedQuery);
        return new FeedbackSummary(normalizedQuery, summary.averageScore(), summary.ratingCount(), score);
    }

    public FeedbackSummary getByQuery(String query) {
        String normalizedQuery = normalizeQuery(query);
        return modelFeedbackRepository.getByQuery(modelName, normalizedQuery);
    }

    public FeedbackOverallSummary getOverall() {
        return modelFeedbackRepository.getOverall(modelName);
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

    private String normalizeSortOption(String sortOption) {
        if (sortOption == null || sortOption.isBlank()) {
            return "RELEVANCE_DESC";
        }
        return sortOption.trim();
    }

    private long normalizeSearchDurationMillis(Long searchDurationMillis) {
        if (searchDurationMillis == null || searchDurationMillis < 0) {
            return 0L;
        }
        return searchDurationMillis;
    }
}
