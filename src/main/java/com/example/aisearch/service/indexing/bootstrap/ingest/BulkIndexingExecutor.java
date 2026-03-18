package com.example.aisearch.service.indexing.bootstrap.ingest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class BulkIndexingExecutor {

    private static final Logger log = LoggerFactory.getLogger(BulkIndexingExecutor.class);

    private final ElasticsearchClient client;

    public BulkIndexingExecutor(ElasticsearchClient client) {
        this.client = client;
    }

    public long bulkIndex(String indexName, List<IndexDocument> documents) {
        if (documents.isEmpty()) {
            return 0;
        }

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder().index(indexName);
        for (IndexDocument doc : documents) {
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .id(doc.id())
                            .document(doc.document())
                    )
            );
        }

        try {
            /*
              1. refresh=wait_for
                - 색인 후 “검색 가능해질 때까지” 응답을 기다립니다.
                - 실서비스에서 가장 무난한 선택입니다.
             */
            var response = client.bulk(bulkBuilder.refresh(Refresh.WaitFor).build());
            if (response.errors()) {
                throw new IllegalStateException("Bulk 인덱싱 중 일부 실패: " + summarizeBulkErrors(response));
            }
            return documents.size();
        } catch (IOException e) {
            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                message = e.getClass().getSimpleName();
            }
            log.error("Bulk indexing request failed. indexName={}, documentCount={}, message={}",
                    indexName, documents.size(), message, e);
            throw new IllegalStateException("Bulk 인덱싱 실패: " + message, e);
        }
    }

    private String summarizeBulkErrors(co.elastic.clients.elasticsearch.core.BulkResponse response) {
        return response.items().stream()
                .filter(item -> item.error() != null)
                .limit(3)
                .map(item -> "id=" + item.id()
                        + ", type=" + item.error().type()
                        + ", reason=" + item.error().reason())
                .reduce((a, b) -> a + " | " + b)
                .orElse("원인 미확인");
    }
}
