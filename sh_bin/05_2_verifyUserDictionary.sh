#!/usr/bin/env bash
set -euo pipefail

# 실행 환경 기본값
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common/port_forward.sh"

NAMESPACE="${NAMESPACE:-ai-search}"
SECRET_NAME="${SECRET_NAME:-ai-search-es-es-elastic-user}"
SERVICE="${SERVICE:-}"
LOCAL_PORT="${LOCAL_PORT:-9200}"

ANALYZE_INDEX="${ANALYZE_INDEX:-food-products-read}"
ANALYZER_NAME="${ANALYZER_NAME:-ko_mall_search_analyzer}"
ANALYZE_TEXT="${ANALYZE_TEXT:-얇은피 만두}"
if [ "${ANALYZE_EXPECTED_TOKEN+x}" = "x" ]; then
  ANALYZE_EXPECTED_TOKEN="${ANALYZE_EXPECTED_TOKEN}"
else
  ANALYZE_EXPECTED_TOKEN="얇은피"
fi
ANALYZE_EXPECTED_TOKENS="${ANALYZE_EXPECTED_TOKENS:-}"
ANALYZE_UNEXPECTED_TOKENS="${ANALYZE_UNEXPECTED_TOKENS:-}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[오류] kubectl 명령어를 찾을 수 없습니다."
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[오류] curl 명령어를 찾을 수 없습니다."
  exit 1
fi

# Kubernetes Secret에서 elastic 비밀번호 로드
PASSWORD="$(kubectl get secret "${SECRET_NAME}" -n "${NAMESPACE}" -o go-template='{{.data.elastic | base64decode}}')"
echo "[정보] Secret에서 elastic 비밀번호를 읽었습니다."

# 서비스명을 지정하지 않으면 -es-http 서비스 자동 탐지
if [ -z "${SERVICE}" ]; then
  SERVICE="$(find_es_http_service "${NAMESPACE}")"
fi

if [ -z "${SERVICE}" ]; then
  echo "[오류] namespace ${NAMESPACE}에서 Elasticsearch HTTP 서비스를 찾지 못했습니다."
  exit 1
fi

echo "[정보] 사용할 서비스: ${SERVICE}"
start_port_forward "${NAMESPACE}" "${SERVICE}" "${LOCAL_PORT}" 9200 "/tmp/ai-search-userdict-verify-port-forward.log"
trap cleanup_port_forward EXIT

# _analyze 호출로 토큰 결과 확인
ANALYZE_RESPONSE="$(
  curl -sS -u "elastic:${PASSWORD}" \
    -X POST "http://localhost:${LOCAL_PORT}/${ANALYZE_INDEX}/_analyze" \
    -H "Content-Type: application/json" \
    -d "{\"analyzer\":\"${ANALYZER_NAME}\",\"text\":\"${ANALYZE_TEXT}\"}"
)"

echo "${ANALYZE_RESPONSE}"

if ! echo "${ANALYZE_RESPONSE}" | grep -q '"tokens"'; then
  echo "[오류] _analyze 응답에 tokens 필드가 없습니다."
  exit 1
fi

if [ -n "${ANALYZE_EXPECTED_TOKEN}" ] && ! echo "${ANALYZE_RESPONSE}" | grep -q "\"token\":\"${ANALYZE_EXPECTED_TOKEN}\""; then
  echo "[오류] 기대 토큰을 찾지 못했습니다: ${ANALYZE_EXPECTED_TOKEN}"
  exit 1
fi

if [ -n "${ANALYZE_EXPECTED_TOKENS}" ]; then
  IFS=',' read -r -a expected_tokens <<< "${ANALYZE_EXPECTED_TOKENS}"
  for token in "${expected_tokens[@]}"; do
    trimmed="$(echo "${token}" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    if [ -n "${trimmed}" ] && ! echo "${ANALYZE_RESPONSE}" | grep -q "\"token\":\"${trimmed}\""; then
      echo "[오류] 기대 토큰을 찾지 못했습니다: ${trimmed}"
      exit 1
    fi
  done
fi

if [ -n "${ANALYZE_UNEXPECTED_TOKENS}" ]; then
  IFS=',' read -r -a unexpected_tokens <<< "${ANALYZE_UNEXPECTED_TOKENS}"
  for token in "${unexpected_tokens[@]}"; do
    trimmed="$(echo "${token}" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
    if [ -n "${trimmed}" ] && echo "${ANALYZE_RESPONSE}" | grep -q "\"token\":\"${trimmed}\""; then
      echo "[오류] 나오면 안 되는 토큰이 발견되었습니다: ${trimmed}"
      exit 1
    fi
  done
fi

echo "[완료] 사용자 사전 검증이 완료되었습니다."

# 상용에서 배치에서 실행되게 처리