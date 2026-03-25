package com.example.aisearch.service.feedback;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ModelFeedbackRepository {

    private final JdbcTemplate jdbcTemplate;

    public ModelFeedbackRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String modelName, String query, int score, String sortOption, long searchDurationMillis) {
        jdbcTemplate.update(
                """
                insert into model_feedback_event (
                    query_text,
                    sort_option,
                    model_name,
                    score,
                    search_duration_millis
                ) values (?, ?, ?, ?, ?)
                """,
                query,
                sortOption,
                modelName,
                score,
                searchDurationMillis
        );
    }

    public FeedbackSummary getByQuery(String modelName, String query) {
        return jdbcTemplate.query(
                """
                select
                    coalesce(avg(score), 0) as average_score,
                    count(*) as rating_count
                from model_feedback_event
                where model_name = ?
                  and query_text = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return new FeedbackSummary(query, 0.0, 0, 0);
                    }
                    return new FeedbackSummary(
                            query,
                            rs.getDouble("average_score"),
                            rs.getLong("rating_count"),
                            0
                    );
                },
                modelName,
                query
        );
    }

    public FeedbackOverallSummary getOverall(String modelName) {
        ModelFeedbackAggregate aggregate = getAggregateByModel(modelName);
        return new FeedbackOverallSummary(aggregate.averageScore(), aggregate.ratingCount());
    }

    private ModelFeedbackAggregate getAggregateByModel(String modelName) {
        return jdbcTemplate.query(
                """
                select
                    coalesce(avg(score), 0) as average_score,
                    count(*) as rating_count
                from model_feedback_event
                where model_name = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return new ModelFeedbackAggregate(0.0, 0);
                    }
                    return new ModelFeedbackAggregate(
                            rs.getDouble("average_score"),
                            rs.getLong("rating_count")
                    );
                },
                modelName
        );
    }

    private record ModelFeedbackAggregate(
            double averageScore,
            long ratingCount
    ) {
    }
}
