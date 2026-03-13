# HybridBaseQueryBuilder 쿼리 구성 설명

이 문서는 `HybridBaseQueryBuilder#build(ProductSearchRequest request, Optional<Query> filterQuery)`가 Elasticsearch JSON 쿼리로 어떻게 변환되는지 단계적으로 설명한다.

## 1) 메서드 목적
- 하이브리드 검색에서 `script_score`의 내부 `query`로 사용할 **base bool query**를 만든다.
- 구성 요소:
  - lexical query(`multi_match`)
  - optional filter query(`filter`)
  - `minimum_should_match = 0` 설정

## 2) 입력값 예시
- `request.query()` = `"사과 주스"`
- `filterQuery` = 카테고리/가격 조건이 들어있는 `Query` (있을 수도, 없을 수도 있음)

---

## 3) 1단계: lexicalQuery 생성
코드:
```java
Query lexicalQuery = Query.of(q -> q.multiMatch(mm -> mm
        .query(request.query())
        .fields("product_name^2", "description")
));
```

생성되는 JSON(개념상):
```json
{
  "multi_match": {
    "query": "사과 주스",
    "fields": ["product_name^2", "description"]
  }
}
```

의미:
- `product_name` 필드에 가중치 `^2`를 적용해 `description`보다 더 중요하게 반영한다.

---

## 4) 2단계: bool query 생성
코드:
```java
return Query.of(q -> q.bool(b -> {
    filterQuery.ifPresent(b::filter);
    b.should(lexicalQuery);
    b.minimumShouldMatch("0");
    return b;
}));
```

핵심 동작:
- `filterQuery`가 있으면 `bool.filter`에 추가
- `lexicalQuery`는 `bool.should`에 추가
- `minimum_should_match("0")`로 should 미일치도 허용

왜 `0`인가:
- 주석의 vector-first 전략처럼, 텍스트 매칭이 약해도 벡터 유사도/필터 조건을 기준으로 후보를 유지하려는 의도다.

---

## 5) 3단계: 최종 JSON 형태

### 5-1. filterQuery가 없는 경우
```json
{
  "bool": {
    "should": [
      {
        "multi_match": {
          "query": "사과 주스",
          "fields": ["product_name^2", "description"]
        }
      }
    ],
    "minimum_should_match": "0"
  }
}
```

### 5-2. filterQuery가 있는 경우
(예: 카테고리 + 가격 필터가 있다고 가정)

설명:
- `bool.filter`에 복수 조건이 들어가면 **AND**로 동작한다.
- 즉, 아래 예시는 `categoryId` 조건과 `price` 조건을 **모두 만족**하는 문서만 통과한다.

```json
{
  "bool": {
    "filter": [
      { "terms": { "categoryId": [4, 8] } },
      { "range": { "price": { "gte": 1000, "lte": 30000 } } }
    ],
    "should": [
      {
        "multi_match": {
          "query": "사과 주스",
          "fields": ["product_name^2", "description"]
        }
      }
    ],
    "minimum_should_match": "0"
  }
}
```

주의:
- 실제 `filter` 내부 구조는 `SearchFilterQueryBuilder`가 만드는 Query 내용에 따라 달라진다.
- 이 클래스는 필터를 "받아서 넣는 역할"만 담당한다.

---

## 6) 전체 흐름 한 줄 요약
1. 검색어로 `multi_match`를 만든다.
2. optional 필터를 `bool.filter`에 붙인다.
3. `multi_match`를 `bool.should`에 넣고 `minimum_should_match=0`으로 후보를 넓힌다.
4. 이 bool query가 이후 하이브리드(script_score) 계산의 base query로 사용된다.
