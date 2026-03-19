package com.example.aisearch;

import com.example.aisearch.model.SearchHitResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public final class SearchResultTestSupport {

    private SearchResultTestSupport() {
    }

    public static Integer asInteger(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    public static List<String> extractIds(List<SearchHitResult> results) {
        return results.stream().map(SearchHitResult::id).toList();
    }

    public static List<Integer> extractIntegers(List<SearchHitResult> results, String key) {
        return results.stream()
                .map(hit -> asInteger(hit.source(), key))
                .filter(value -> value != null)
                .toList();
    }

    public static boolean containsCategoryId(Map<String, Object> source, String key, int expectedCategoryId) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue() == expectedCategoryId;
        }
        if (value instanceof List<?> values) {
            return values.stream()
                    .filter(Number.class::isInstance)
                    .map(Number.class::cast)
                    .map(Number::intValue)
                    .anyMatch(categoryId -> categoryId == expectedCategoryId);
        }
        return false;
    }

    public static void printResults(String label, List<SearchHitResult> results) {
        System.out.println("[SEARCH_RESULTS] label=" + label + ", size=" + results.size());
        for (int i = 0; i < results.size(); i++) {
            SearchHitResult hit = results.get(i);
            System.out.println("rank=" + (i + 1)
                    + ", score=" + hit.score()
                    + ", id=" + hit.id()
                    + ", name=" + hit.source().get("goods_name")
                    + ", categoryIds=" + hit.source().get("lev3_category_id")
                    + ", category=" + hit.source().get("lev3_category_id_name")
                    + ", price=" + hit.source().get("sale_price"));
        }
    }

    public static boolean containsProductName(JsonNode results, String keyword) {
        for (JsonNode hit : results) {
            String name = hit.path("source").path("goods_name").asText("");
            if (name.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
