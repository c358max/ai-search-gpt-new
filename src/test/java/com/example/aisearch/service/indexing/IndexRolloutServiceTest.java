package com.example.aisearch.service.indexing;

import com.example.aisearch.service.indexing.bootstrap.ingest.ProductIndexingService;
import com.example.aisearch.service.indexing.domain.AliasSwitcher;
import com.example.aisearch.service.indexing.domain.IndexCleanupService;
import com.example.aisearch.service.indexing.domain.IndexCreator;
import com.example.aisearch.service.indexing.orchestration.IndexRolloutService;
import com.example.aisearch.service.indexing.orchestration.result.IndexRolloutResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexRolloutServiceTest {

    @Mock
    private IndexCreator indexCreator;

    @Mock
    private AliasSwitcher aliasSwitcher;

    @Mock
    private IndexCleanupService indexCleanupService;

    @Mock
    private ProductIndexingService productIndexingService;

    @InjectMocks
    private IndexRolloutService indexRolloutService;

    @Test
    void cleanup이_실패해도_alias_전환까지_성공하면_rollout은_성공으로_반환한다() {
        when(aliasSwitcher.findCurrentAliasedIndex()).thenReturn("products-v20260320090000");
        when(indexCreator.createVersionedIndex()).thenReturn("products-v20260320100000");
        when(productIndexingService.reindexData("products-v20260320100000", null)).thenReturn(210L);
        doThrow(new IllegalStateException("cleanup failed"))
                .when(indexCleanupService)
                .cleanupOldVersionedIndices("products-v20260320100000");

        IndexRolloutResult result = indexRolloutService.rollOutFromSourceData();

        assertTrue(result.rolloutSucceeded());
        assertEquals("products-v20260320090000", result.oldIndex());
        assertEquals("products-v20260320100000", result.newIndex());
        assertEquals(210L, result.indexedCount());
        assertFalse(result.cleanupSucceeded());
        assertEquals(List.of(), result.cleanupDeletedIndices());
        assertEquals("cleanup failed", result.cleanupErrorMessage());

        verify(aliasSwitcher).swapReadAlias("products-v20260320090000", "products-v20260320100000");
        verify(productIndexingService).reindexData("products-v20260320100000", null);
    }

    @Test
    void cleanup이_성공하면_삭제된_인덱스목록을_결과에_포함한다() {
        when(aliasSwitcher.findCurrentAliasedIndex()).thenReturn("products-v20260320090000");
        when(indexCreator.createVersionedIndex()).thenReturn("products-v20260320100000");
        when(productIndexingService.reindexData(anyString(), isNull())).thenReturn(210L);
        when(indexCleanupService.cleanupOldVersionedIndices("products-v20260320100000"))
                .thenReturn(new IndexCleanupService.IndexCleanupResult(
                        4,
                        List.of("products-v20260319080000")
                ));

        IndexRolloutResult result = indexRolloutService.rollOutFromSourceData();

        assertTrue(result.rolloutSucceeded());
        assertTrue(result.cleanupSucceeded());
        assertEquals(List.of("products-v20260319080000"), result.cleanupDeletedIndices());
        assertEquals(null, result.cleanupErrorMessage());
    }
}
