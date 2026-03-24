package com.example.aisearch.service.synonym;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.synonyms.SynonymRule;
import co.elastic.clients.transport.TransportException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class ElasticsearchSynonymEsGateway implements SynonymEsGateway {

    private final ElasticsearchClient client;
    private final RestClient restClient;

    public ElasticsearchSynonymEsGateway(ElasticsearchClient client, RestClient restClient) {
        this.client = client;
        this.restClient = restClient;
    }

    @Override
    public boolean existsSynonyms(String synonymsSetId) {
        try {
            // getSynonym 응답 디코드 이슈를 피하기 위해 존재 여부만 저수준 HTTP status로 확인한다.
            restClient.performRequest(new Request("GET", "/_synonyms/" + synonymsSetId));
            return true;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            }
            throw new IllegalStateException(buildExistsFailureMessage(synonymsSetId, e), e);
        } catch (TransportException e) {
            if (isNotFound(e)) {
                return false;
            }
            throw new IllegalStateException("동의어 세트 조회 실패: " + synonymsSetId, e);
        } catch (IOException | ElasticsearchException e) {
            if (isNotFound(e)) {
                return false;
            }
            throw new IllegalStateException("동의어 세트 조회 실패: " + synonymsSetId, e);
        }
    }

    @Override
    public void putSynonyms(String synonymsSetId, List<String> rules) {
        try {
            client.synonyms().putSynonym(request -> request
                    .id(synonymsSetId)
                    .synonymsSet(toSynonymRules(rules))
            );
        } catch (TransportException e) {
            if (isKnownPutSynonymDecodeIssue(e)) {
                return;
            }
            throw new IllegalStateException("동의어 세트 반영 실패: " + synonymsSetId, e);
        } catch (IOException | ElasticsearchException e) {
            throw new IllegalStateException("동의어 세트 반영 실패: " + synonymsSetId, e);
        }
    }

    @Override
    public void reloadSearchAnalyzers(String indexName) {
        try {
            client.indices().reloadSearchAnalyzers(request -> request.index(indexName));
        } catch (IOException | ElasticsearchException e) {
            throw new IllegalStateException("failed to reload analyzers for index " + indexName, e);
        }
    }

    private List<SynonymRule> toSynonymRules(List<String> rules) {
        return IntStream.range(0, rules.size())
                .mapToObj(i -> SynonymRule.of(rule -> rule
                        .id("rule-" + (i + 1))
                        .synonyms(rules.get(i))
                ))
                .toList();
    }

    private boolean isKnownPutSynonymDecodeIssue(TransportException e) {
        String topMessage = e.getMessage();
        if (topMessage == null) {
            return false;
        }

        boolean topLevelMatches = (topMessage.contains("status: 200") || topMessage.contains("status: 201"))
                && topMessage.contains("es/synonyms.put_synonym")
                && topMessage.contains("Failed to decode response");
        if (!topLevelMatches) {
            return false;
        }

        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && msg.contains("Missing required property 'ReloadDetails.index'")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String buildExistsFailureMessage(String synonymsSetId, ResponseException e) {
        int status = e.getResponse().getStatusLine().getStatusCode();
        String body = "<empty>";
        try {
            // Elasticsearch가 내려준 실제 응답 본문을 함께 남겨야 shard/license 문제를 빠르게 진단할 수 있다.
            if (e.getResponse().getEntity() != null) {
                body = EntityUtils.toString(e.getResponse().getEntity());
            }
        } catch (IOException ignored) {
            body = "<failed to read body>";
        }
        return "동의어 세트 조회 실패: " + synonymsSetId + " status=" + status + " body=" + body;
    }

    private boolean isNotFound(Exception e) {
        String message = e.getMessage();
        if (message != null && (message.contains("status: 404") || message.contains("[404]") || message.contains("404 Not Found"))) {
            return true;
        }

        Throwable current = e;
        while (current != null) {
            String currentMessage = current.getMessage();
            if (currentMessage != null
                    && (currentMessage.contains("status: 404")
                    || currentMessage.contains("[404]")
                    || currentMessage.contains("404 Not Found")
                    || currentMessage.contains("resource_not_found_exception"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
