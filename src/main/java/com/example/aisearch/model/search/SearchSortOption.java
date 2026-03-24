package com.example.aisearch.model.search;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;

import java.util.List;

public enum SearchSortOption {
    RELEVANCE_DESC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    scoreSort(SortOrder.Desc),
                    fieldSort("id", SortOrder.Asc)
            );
        }
    },
    PRICE_ASC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    numericFieldSort("sale_price", SortOrder.Asc),
                    scoreSort(SortOrder.Desc),
                    fieldSort("id", SortOrder.Asc)
            );
        }
    },
    PRICE_DESC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    numericFieldSort("sale_price", SortOrder.Desc),
                    scoreSort(SortOrder.Desc),
                fieldSort("id", SortOrder.Asc)
            );
        }
    },
    POPULARITY_DESC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    numericFieldSort("popularity_score", SortOrder.Desc),
                    scoreSort(SortOrder.Desc),
                    fieldSort("id", SortOrder.Asc)
            );
        }
    },
    DISCOUNT_RATE_DESC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    numericFieldSort("discount_rate", SortOrder.Desc),
                    scoreSort(SortOrder.Desc),
                    fieldSort("id", SortOrder.Asc)
            );
        }
    },
    REVIEW_COUNT_DESC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    numericFieldSort("satisfaction_count", SortOrder.Desc),
                    scoreSort(SortOrder.Desc),
                    fieldSort("id", SortOrder.Asc)
            );
        }
    },
    NEW_GOODS_DESC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    fieldSort("is_new_goods", SortOrder.Desc),
                    scoreSort(SortOrder.Desc),
                    fieldSort("id", SortOrder.Asc)
            );
        }
    },
    RATING_DESC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    numericFieldSort("satisfaction_score", SortOrder.Desc),
                    scoreSort(SortOrder.Desc),
                    fieldSort("id", SortOrder.Asc)
            );
        }
    },
    CATEGORY_BOOSTING_DESC {
        @Override
        public List<SortOptions> toSortOptions() {
            return List.of(
                    scoreSort(SortOrder.Desc),
                    fieldSort("id", SortOrder.Asc)
            );
        }
    }
//    ,
//    RELEVANCE_ASC {
//        @Override
//        public List<SortOptions> toSortOptions() {
//            return List.of(
//                    scoreSort(SortOrder.Asc),
//                    fieldSort("id", SortOrder.Asc)
//            );
//        }
//    }
    ;

    public abstract List<SortOptions> toSortOptions();

    private static SortOptions numericFieldSort(String field, SortOrder order) {
        return fieldSort(field, order);
    }

    private static SortOptions fieldSort(String field, SortOrder order) {
        return SortOptions.of(s -> s.field(f -> f
                .field(field)
                .order(order)
        ));
    }

    private static SortOptions scoreSort(SortOrder order) {
        return SortOptions.of(s -> s.score(sc -> sc.order(order)));
    }
}
