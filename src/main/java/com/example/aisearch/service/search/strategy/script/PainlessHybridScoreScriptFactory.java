package com.example.aisearch.service.search.strategy.script;

import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingResult;
import org.springframework.stereotype.Component;

/**
 * 하이브리드 점수 계산에 사용하는 Painless 스크립트를 제공한다.
 */
@Component
public class PainlessHybridScoreScriptFactory {

    private static final String SCRIPT_COMMON = """
            double vectorScore = (cosineSimilarity(params.query_vector, 'product_vector') + 1.0) / 2.0;
            double lexicalScore = Math.min(_score, 5.0) / 5.0;
            double base = 0.9 * vectorScore + 0.1 * lexicalScore;
            if (base < params.min_score_threshold) return 0.0;
            double categoryBoost = 0.0;
            """;

    private static final String CATEGORY_BOOST_BLOCK = """
            if (doc['primary_lev3_category_id'].size() != 0) {
              String categoryKey = String.valueOf(doc['primary_lev3_category_id'].value);
              def rawBoost = params.category_boost_by_id.get(categoryKey);
              if (rawBoost != null) {
                categoryBoost = ((Number) rawBoost).doubleValue();
              }
            }
            """;

    private static final String SCRIPT_RETURN_BLOCK = """
            // 변수명은 중요하지 않고, return 값이 Elasticsearch의 최종 _score로 사용된다.
            double finalScore = base * (1.0 + params.beta * categoryBoost);
            return finalScore;
            """;

    private static final String BASE_SCRIPT = SCRIPT_COMMON + SCRIPT_RETURN_BLOCK;
    private static final String CATEGORY_BOOST_SCRIPT = SCRIPT_COMMON + CATEGORY_BOOST_BLOCK + SCRIPT_RETURN_BLOCK;

    public String selectScriptSource(CategoryBoostingResult decision) {
        return decision.applyCategoryBoost() ? CATEGORY_BOOST_SCRIPT : BASE_SCRIPT;
    }
}
