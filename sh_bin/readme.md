# Elasticsearch 무료 벡터 검색 로컬 구성

## 1회성 스크립트 (필요할 때만)
### Elasticsearch 준비
```bash
./sh_bin/00_1_delete_elasticsearch_resources.sh
./sh_bin/00_2_install_eck_operator.sh
./sh_bin/00_3_build_elasticsearch_nori_image.sh
./sh_bin/00_4_push_elasticsearch_nori_image.sh
./sh_bin/00_6_start_elasticsearch_cluster_custom_image.sh
./sh_bin/00_9_check_elasticsearch_nori_plugin.sh
```

### DJL 준비
```bash
./sh_bin/10_1_prepare_djl_truststore.sh
./sh_bin/10_2_check_djl_truststore.sh
./sh_bin/10_3_check_eck_operator_status.sh
```
`10_1` 실행 후 truststore는 `~/.ai-cert/djl-truststore.p12`에 생성됩니다.

## Nori 커스텀 이미지 경로 (선택)
`analysis-nori`를 클러스터 내부에서 다운로드하지 않고, 커스텀 이미지에 내장해서 사용합니다.

```bash
./sh_bin/00_3_build_elasticsearch_nori_image.sh
./sh_bin/00_4_push_elasticsearch_nori_image.sh
./sh_bin/00_6_start_elasticsearch_cluster_custom_image.sh
./sh_bin/00_9_check_elasticsearch_nori_plugin.sh
```

기본 동작은 로컬 태그(`ai-search-es:8.13.4-nori`)를 사용합니다.
레지스트리에 push가 필요한 경우에만 `ES_CUSTOM_IMAGE=<registry>/<repo>/ai-search-es:8.13.4-nori`를 지정하세요.
인증서 이슈가 있는 네트워크에서는 `CURL_INSECURE=true ./sh_bin/00_3_build_elasticsearch_nori_image.sh`를 사용할 수 있습니다.

`00_3`에서 플러그인 다운로드가 실패하면, 아래 파일을 직접 받아 지정 위치에 넣은 뒤 다시 `00_3`을 실행하세요.
- URL: `https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-nori/analysis-nori-8.13.4.zip`
- 저장 위치: `sh_bin/analysis-nori-8.13.4.zip`

## 일반 실행 순서 (매번/자주 실행)
### 1) Elasticsearch 상태/라이선스 확인

```bash
./sh_bin/01_check_elasticsearch_status.sh
```
`_license` 결과에서 `"type" : "basic"`이면 무료 라이선스 모드입니다.

### 2) 샘플 데이터 생성
```bash
./sh_bin/02_generate_sample_data.sh
```

### 3) 샘플 데이터 확인
```bash
./sh_bin/03_check_sample_data.sh
```

### 4) 벡터 검색 테스트 실행
```bash
./sh_bin/04_run_vector_search_test_local.sh
```

## 운영 상태 빠른 확인
```bash
./sh_bin/90_check_k8s_elastic_pods.sh
```
