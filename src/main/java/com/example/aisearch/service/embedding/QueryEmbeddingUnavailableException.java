package com.example.aisearch.service.embedding;

/**
 * 검색어 임베딩을 제한 시간 내에 생성하지 못했거나 생성 과정이 실패했음을 나타낸다.
 */
public class QueryEmbeddingUnavailableException extends RuntimeException {

    public QueryEmbeddingUnavailableException(String message) {
        super(message);
    }

    public QueryEmbeddingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
