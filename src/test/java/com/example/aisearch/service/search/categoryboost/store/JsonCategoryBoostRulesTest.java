package com.example.aisearch.service.search.categoryboost.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonCategoryBoostRulesTest {

    @Test
    void shouldLoadRulesFromFixtureV1() {
        JsonCategoryBoostRules rules = new JsonCategoryBoostRules(
                new DefaultResourceLoader(),
                new ObjectMapper(),
                "classpath:data/category_boosting_v1.json",
                300
        );

        Optional<Map<String, Double>> apple = rules.findByKeyword("사과");
        assertTrue(apple.isPresent());
        assertEquals(0.20, apple.get().get("4"));
    }

    @Test
    void shouldReloadRulesWhenVersionChangesByPathSwitching() {
        JsonCategoryBoostRules rules = new JsonCategoryBoostRules(
                new DefaultResourceLoader(),
                new ObjectMapper(),
                "classpath:data/category_boosting_v1.json",
                300
        );

        assertEquals(0.20, rules.findByKeyword("사과").orElseThrow().get("4"));

        rules.setRuleFilePath("classpath:data/category_boosting_v2.json");
        rules.reload();

        assertEquals(0.30, rules.findByKeyword("사과").orElseThrow().get("4"));
    }

    @Test
    void shouldLoadRulesFromArrayBasedCategoryBoostJson() {
        JsonCategoryBoostRules rules = new JsonCategoryBoostRules(
                new DefaultResourceLoader(),
                new ObjectMapper(),
                "classpath:data/category_boost.json",
                300
        );

        Map<String, Double> 건강 = rules.findByKeyword("건강").orElseThrow();
        assertEquals(0.9, 건강.get("5102"));
        assertEquals(0.9, 건강.get("5108"));
        assertEquals(0.9, 건강.get("5110"));

        Map<String, Double> 간장 = rules.findByKeyword("간장").orElseThrow();
        assertEquals(0.3, 간장.get("5081"));
    }
}
