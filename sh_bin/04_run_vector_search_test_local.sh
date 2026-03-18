#!/usr/bin/env bash
set -euo pipefail

# 기본 실행 환경 값
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${SCRIPT_DIR}/common/port_forward.sh"

NAMESPACE="ai-search"
SERVICE="${SERVICE:-}"
LOCAL_PORT="9200"
TEST_CLASS_PATTERN="*VectorSearchIntegrationTest"
TRUSTSTORE_PATH="${HOME}/.ai-cert/djl-truststore.p12"
TRUSTSTORE_PASSWORD="${AI_SEARCH_TRUSTSTORE_PASSWORD:-changeit}"

# 필요한 명령어가 없으면 바로 종료합니다.
if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

if [ ! -x "${ROOT_DIR}/gradlew" ]; then
  echo "[ERROR] ./gradlew not found or not executable"
  exit 1
fi

# 가능하면 Java 21을 자동 선택합니다.
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  if JAVA21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null); then
    export JAVA_HOME="${JAVA21_HOME}"
    echo "[INFO] JAVA_HOME set to Java 21 (${JAVA_HOME})"
  fi
fi

# truststore는 10_1 스크립트에서 미리 생성해 둡니다.
if [ ! -f "${TRUSTSTORE_PATH}" ]; then
  echo "[ERROR] truststore not found: ${TRUSTSTORE_PATH}"
  echo "[NEXT] Run: ./sh_bin/10_1_prepare_djl_truststore.sh"
  exit 1
fi

# Elasticsearch 비밀번호를 Secret에서 읽습니다.
PASSWORD=$(kubectl get secret ai-search-es-es-elastic-user -n "${NAMESPACE}" -o go-template='{{.data.elastic | base64decode}}')
echo "[INFO] elastic password loaded from secret"

# SERVICE를 지정하지 않으면 -es-http 서비스 이름을 자동으로 찾습니다.
if [ -z "${SERVICE}" ]; then
  SERVICE="$(find_es_http_service "${NAMESPACE}")"
fi

if [ -z "${SERVICE}" ]; then
  echo "[ERROR] Elasticsearch http service not found in namespace ${NAMESPACE}"
  exit 1
fi

echo "[INFO] using service ${SERVICE}"

# 테스트 중 Elasticsearch에 접속할 수 있게 로컬 포트포워딩을 엽니다.
start_port_forward "${NAMESPACE}" "${SERVICE}" "${LOCAL_PORT}" 9200 "/tmp/ai-search-local-test-port-forward.log"
trap cleanup_port_forward EXIT

# truststore를 JVM에 적용하고 통합 테스트를 실행합니다.
echo "[INFO] running JUnit test locally (${TEST_CLASS_PATTERN})"
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=${TRUSTSTORE_PATH} -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD}"
AI_SEARCH_ES_URL="http://localhost:${LOCAL_PORT}" \
AI_SEARCH_ES_USERNAME="elastic" \
AI_SEARCH_ES_PASSWORD="${PASSWORD}" \
"${ROOT_DIR}/gradlew" test --tests "${TEST_CLASS_PATTERN}"

echo "[OK] local test finished"

# 안씀.