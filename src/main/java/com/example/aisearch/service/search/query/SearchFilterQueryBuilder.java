package com.example.aisearch.service.search.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.aisearch.model.search.SearchPrice;
import com.example.aisearch.model.search.ProductSearchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SearchFilterQueryBuilder {

    public Optional<Query> buildFilterQuery(ProductSearchRequest request) {
        List<Query> filters = new ArrayList<>();
        addPriceFilter(request).ifPresent(filters::add);
        addCategoryFilter(request).ifPresent(filters::add);

        // 필터가 하나도 없으면 "필터 조건 없음"을 의미하도록 빈 Optional을 반환한다.
        if (filters.isEmpty()) {
            return Optional.empty();
        }
        // 여러 필터는 bool.filter로 묶인다(점수 가산 없이 조건 통과 여부만 판단).
        return Optional.of(Query.of(q -> q.bool(b -> b.filter(filters))));
    }

    public Query buildRootQuery(ProductSearchRequest request) {
        // 필터가 있으면 필터 쿼리를 루트로 사용하고, 없으면 전체 문서를 대상으로 검색한다.
        return buildFilterQuery(request)
                .orElseGet(() -> Query.of(q -> q.matchAll(m -> m)));
    }

    private Optional<Query> addPriceFilter(ProductSearchRequest request) {
        if (!request.hasPriceCondition()) {
            return Optional.empty();
        }
        SearchPrice price = request.searchPrice();
        Query priceFilter = Query.of(q -> q.range(r -> {
            r.field("sale_price");
            if (price.minPrice() != null) {
                // gte: 최소 가격 이상
                r.gte(JsonData.of(price.minPrice()));
            }
            if (price.maxPrice() != null) {
                // lte: 최대 가격 이하
                r.lte(JsonData.of(price.maxPrice()));
            }
            return r;
        }));
        return Optional.of(priceFilter);
    }

    private Optional<Query> addCategoryFilter(ProductSearchRequest request) {
        if (!request.hasCategoryCondition()) {
            return Optional.empty();
        }
        List<FieldValue> values = request.categoryIds().stream()
                .map(FieldValue::of)
                .toList();

        // terms는 SQL의 IN 절과 유사하다: categoryId IN (...)
        Query categoryFilter = Query.of(q -> q.terms(t -> t
                .field("lev3_category_id")
                .terms(tf -> tf.value(values))

        ));
        return Optional.of(categoryFilter);
    }
}
