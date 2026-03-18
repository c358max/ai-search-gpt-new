package com.example.aisearch.service.synonym;

import java.util.List;

public interface SynonymEsGateway {
    boolean existsSynonyms(String synonymsSetId);

    void putSynonyms(String synonymsSetId, List<String> rules);

    void reloadSearchAnalyzers(String indexName);
}
