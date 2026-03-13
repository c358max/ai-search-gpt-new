package com.example.aisearch;

import com.example.aisearch.service.indexing.orchestration.IndexRolloutService;
import com.example.aisearch.service.synonym.SynonymReloadMode;
import com.example.aisearch.support.RequiresElasticsearch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RequiresElasticsearch
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SynonymsRestClientIntegrationTest extends TruststoreTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private IndexRolloutService indexRolloutService;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    void setUp() {
        indexRolloutService.rollOutFromSourceData();
    }

    @AfterAll
    void restoreProductionSynonyms() throws Exception {
        try {
            reloadSynonyms(SynonymReloadMode.PRODUCTION);
        } catch (Exception ignored) {
            // 테스트 본문의 실패 원인을 가리지 않기 위해 정리 단계 예외는 무시
        }
    }

    @Test
    @Order(1)
    void 회귀동의어_적용후_딤섬_검색시_만두가_포함된다() throws Exception {
        reloadSynonymsAndAssert(SynonymReloadMode.REGRESSION);
        assertSynonymSearchContainsProduct("딤섬", "만두");
    }

    @Test
    @Order(2)
    void 동의어_적용후_교자_검색시_만두가_포함된다() throws Exception {
        reloadSynonymsAndAssert(SynonymReloadMode.PRODUCTION);
        assertSynonymSearchContainsProduct("교자", "만두");
    }


    @Test
    @Order(3)
    void 동의어_적용후_얄피_검색시_생만두가_포함된다() throws Exception {
        reloadSynonymsAndAssert(SynonymReloadMode.PRODUCTION);
        assertSynonymSearchContainsProduct("얄피", "만두");
    }

    private HttpResponse<String> reloadSynonyms(SynonymReloadMode mode) throws Exception {
        URI uri = URI.create("http://localhost:" + port + "/api/search/reload-synonyms");
        String body = """
                {
                  "mode": "%s"
                }
                """.formatted(mode.name());

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> search(String query, int size) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create("http://localhost:" + port + "/api/search?q=" + encodedQuery + "&page=1&size=" + size + "&sort=RELEVANCE_DESC");
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode reloadSynonymsAndAssert(SynonymReloadMode mode) throws Exception {
        HttpResponse<String> reloadResponse = reloadSynonyms(mode);
        assertEquals(200, reloadResponse.statusCode());
        JsonNode reloadJson = objectMapper.readTree(reloadResponse.body());
        assertTrue(reloadJson.path("updated").asBoolean(false));
        assertTrue(reloadJson.path("reloaded").asBoolean(false));
        assertEquals(mode.name(), reloadJson.path("mode").asText());
        return reloadJson;
    }

    private JsonNode searchAndAssertOk(String query, int size) throws Exception {
        HttpResponse<String> searchResponse = search(query, size);
        assertEquals(200, searchResponse.statusCode());
        JsonNode searchJson = objectMapper.readTree(searchResponse.body());
        assertTrue(searchJson.path("results").isArray());
        return searchJson;
    }

    private void assertContainsProductName(JsonNode results, String expectedNameKeyword) {
        boolean containsExpectedProduct = false;
        for (JsonNode hit : results) {
            String name = hit.path("source").path("product_name").asText("");
            if (name.contains(expectedNameKeyword)) {
                containsExpectedProduct = true;
                break;
            }
        }
        assertTrue(containsExpectedProduct, "검색 결과에 '" + expectedNameKeyword + "' 상품이 포함되어야 합니다.");
    }

    private void printSearchResults(String query, JsonNode searchJson, String targetKeyword) {
        JsonNode results = searchJson.path("results");
        System.out.println("[SYNONYM_SEARCH] query=" + query
                + ", totalElements=" + searchJson.path("totalElements").asLong()
                + ", totalPages=" + searchJson.path("totalPages").asInt()
                + ", count=" + searchJson.path("count").asInt()
                + ", targetKeyword=" + targetKeyword);

        long printed = java.util.stream.IntStream.range(0, results.size())
                .mapToObj(i -> new HitView(i, results.get(i)))
                .filter(hitView -> hitView.productName().contains(targetKeyword))
                .peek(hitView -> System.out.println("rank=" + hitView.rank()
                        + ", score=" + hitView.hit().path("score").asDouble()
                        + ", id=" + hitView.hit().path("id").asText()
                        + ", name=" + hitView.productName()
                        + ", category=" + hitView.source().path("category").asText()
                        + ", price=" + hitView.source().path("price").asText()))
                .count();

        if (printed == 0) {
            System.out.println("[SYNONYM_SEARCH] target keyword not found in printed results");
        }
    }

    private void assertSynonymSearchContainsProduct(
            String query,
        String expectedProductNameKeyword
    ) throws Exception {
        JsonNode searchJson = searchAndAssertOk(query, 20);
        printSearchResults(query, searchJson, expectedProductNameKeyword);
        assertContainsProductName(searchJson.path("results"), expectedProductNameKeyword);
    }

    private record HitView(int index, JsonNode hit) {
        int rank() {
            return index + 1;
        }

        JsonNode source() {
            return hit.path("source");
        }

        String productName() {
            return source().path("product_name").asText("");
        }
    }
}
