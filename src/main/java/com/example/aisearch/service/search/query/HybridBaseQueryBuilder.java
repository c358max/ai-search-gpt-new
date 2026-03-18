package com.example.aisearch.service.search.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.aisearch.model.search.ProductSearchRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 하이브리드 검색에서 사용할 베이스 bool 쿼리를 생성한다.
 */
@Component
public class HybridBaseQueryBuilder {

    public Query build(ProductSearchRequest request, Optional<Query> filterQuery) {
        Query lexicalQuery = Query.of(q -> q.multiMatch(mm -> mm
                .query(request.query())
                .fields("goods_name^2", "goods_full_name", "search_keyword")
        ));

        return Query.of(q -> q.bool(b -> {
            filterQuery.ifPresent(b::filter);
            b.should(lexicalQuery);
            // vector-first 전략: 텍스트 should가 불일치여도 벡터/필터 기준으로 후보를 허용한다.
            b.minimumShouldMatch("0");
            return b;
        }));
    }

    public Query buildLexicalFallback(ProductSearchRequest request, Optional<Query> filterQuery) {
        Query lexicalQuery = Query.of(q -> q.multiMatch(mm -> mm
                .query(request.query())
                .fields("goods_name^2", "goods_full_name", "search_keyword")
        ));

        return Query.of(q -> q.bool(b -> {
            filterQuery.ifPresent(b::filter);
            b.must(lexicalQuery);
            return b;
        }));
    }
}
