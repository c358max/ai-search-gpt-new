package com.example.aisearch.service.indexing.orchestration.result;

import java.util.List;

public record IndexRolloutResult(
        String oldIndex,
        String newIndex,
        long indexedCount,
        boolean cleanupSucceeded,
        List<String> cleanupDeletedIndices,
        String cleanupErrorMessage
) {
    public boolean rolloutSucceeded() {
        return newIndex != null && !newIndex.isBlank();
    }
}
