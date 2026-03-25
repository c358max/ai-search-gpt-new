package com.example.aisearch.service.feedback;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({ModelFeedbackService.class, ModelFeedbackRepository.class})
@TestPropertySource(properties = "ai-search.model-key=e5-small-ko-v2")
class ModelFeedbackServiceTest {

    @Autowired
    private ModelFeedbackService modelFeedbackService;

    @Test
    void savesRatingsAndAggregatesByQueryAndOverall() {
        FeedbackSummary first = modelFeedbackService.save("만두", 5, "RELEVANCE_DESC", 120L);
        FeedbackSummary second = modelFeedbackService.save("만두", 3, "NEW_GOODS_DESC", 180L);
        modelFeedbackService.save("국", 4, "PRICE_ASC", 95L);

        assertThat(first.savedScore()).isEqualTo(5);
        assertThat(second.averageScore()).isEqualTo(4.0);
        assertThat(second.ratingCount()).isEqualTo(2);

        FeedbackSummary byQuery = modelFeedbackService.getByQuery("만두");
        assertThat(byQuery.averageScore()).isEqualTo(4.0);
        assertThat(byQuery.ratingCount()).isEqualTo(2);

        FeedbackOverallSummary overall = modelFeedbackService.getOverall();
        assertThat(overall.averageScore()).isEqualTo(4.0);
        assertThat(overall.ratingCount()).isEqualTo(3);
    }
}
