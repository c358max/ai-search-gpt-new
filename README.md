# ai-search-gpt

무료 라이선스(Basic) Elasticsearch + Spring Boot 기반 한글 식품 벡터 검색 토이 프로젝트입니다.

## 핵심 구성
- Elasticsearch: ECK로 Kubernetes에 배포
- 라이선스: Basic(무료)
- 임베딩: DJL + `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` (오픈 모델)
- 검색: Elasticsearch `dense_vector` + `knn` 쿼리
- 데이터: `src/main/resources/data/food-products.json` (120건)

## 빠른 시작
1. `./sh_bin/00_2_install_eck_operator.sh` (1회성)
2. `./sh_bin/00_3_build_elasticsearch_nori_image.sh` (1회성)
3. `ES_CUSTOM_IMAGE=<registry>/<repo>/ai-search-es:8.13.4-nori ./sh_bin/00_4_push_elasticsearch_nori_image.sh` (선택)
4. `./sh_bin/00_6_start_elasticsearch_cluster_custom_image.sh`
5. `./sh_bin/10_1_prepare_djl_truststore.sh` (1회성)
6. `./sh_bin/01_check_elasticsearch_status.sh`
7. `./sh_bin/02_generate_sample_data.sh`
8. `./sh_bin/04_run_vector_search_test_local.sh`

상세 절차는 `sh_bin/readme.md` 참고.

## 테스트 실행
- 기본 검증(로컬 ES 없이 가능): `./gradlew test`
- 통합 검증(Elasticsearch 필요): `./gradlew integrationTest`

통합 테스트는 로컬 Elasticsearch, truststore, 샘플 색인 환경이 준비된 상태를 전제로 합니다.
