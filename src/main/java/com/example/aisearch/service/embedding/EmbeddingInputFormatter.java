package com.example.aisearch.service.embedding;

import com.example.aisearch.config.AiSearchProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class EmbeddingInputFormatter {

    private final boolean e5Family;

    public EmbeddingInputFormatter(AiSearchProperties properties) {
        String path = properties.embeddingModelPath();
        String url = properties.embeddingModelUrl();
        String source = ((path == null ? "" : path) + " " + (url == null ? "" : url)).toLowerCase(Locale.ROOT);
        this.e5Family = source.contains("e5");
    }

    public String formatQuery(String query) {
        if (!e5Family) {
            return query;
        }
        return addPrefix("query: ", query);
    }

    public String formatDocument(String text) {
        if (!e5Family) {
            return text;
        }
        return addPrefix("passage: ", text);
    }

    private String addPrefix(String prefix, String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return value;
        }
        return prefix + value;
    }
}
