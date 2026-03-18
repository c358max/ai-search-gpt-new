package com.example.aisearch.support;

import com.example.aisearch.service.embedding.EmbeddingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Primary
@Service
@ConditionalOnProperty(prefix = "ai-search.embedding", name = "provider", havingValue = "stub")
public class TestHashEmbeddingService implements EmbeddingService {

    private static final int DIMENSIONS = 64;

    @Override
    public List<Float> toEmbeddingVector(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String token : tokenize(text)) {
            int tokenHash = token.hashCode();
            int index = Math.floorMod(tokenHash, DIMENSIONS);
            vector[index] += 1.0f;

            int mirrorIndex = Math.floorMod(tokenHash * 31, DIMENSIONS);
            vector[mirrorIndex] += 0.5f;
        }

        normalize(vector);

        List<Float> values = new ArrayList<>(DIMENSIONS);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    private void normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }
}
