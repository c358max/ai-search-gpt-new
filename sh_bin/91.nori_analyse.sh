#!/usr/bin/env bash
set -euo pipefail

#-----[{기본값 설정}]--------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common/port_forward.sh"

ES_URL="${ES_URL:-http://localhost:9200}"
ES_USERNAME="${ES_USERNAME:-elastic}"
ANALYZE_INDEX="${ANALYZE_INDEX:-food-products-read}"
ANALYZER_NAME="${ANALYZER_NAME:-ko_mall_search_analyzer}"
NAMESPACE="${NAMESPACE:-ai-search}"
SECRET_NAME="${SECRET_NAME:-ai-search-es-es-elastic-user}"
SERVICE="${SERVICE:-}"
LOCAL_PORT="${LOCAL_PORT:-9200}"

# 첫 번째 인자로 분석할 텍스트를 받는다.
ANALYZE_TEXT="${1:-}"

#-----[{입력값 검증}]--------------
if [ -z "${ANALYZE_TEXT}" ]; then
  echo "[오류] 분석할 텍스트를 인자로 전달하세요."
  echo "[예시] ./sh_bin/91.nori_analyse.sh '얇은피 만두'"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[오류] curl 명령어를 찾을 수 없습니다."
  exit 1
fi

#-----[{비밀번호 자동 조회}]--------------
if ! command -v kubectl >/dev/null 2>&1; then
  echo "[오류] kubectl 명령어를 찾을 수 없습니다."
  exit 1
fi

ES_PASSWORD="$(kubectl get secret "${SECRET_NAME}" -n "${NAMESPACE}" -o go-template='{{.data.elastic | base64decode}}')"
if [ -z "${ES_PASSWORD}" ]; then
  echo "[오류] secret에서 Elasticsearch 비밀번호를 읽지 못했습니다."
  echo "[확인] namespace=${NAMESPACE}, secret=${SECRET_NAME}"
  exit 1
fi

if [ -z "${SERVICE}" ]; then
  SERVICE="$(find_es_http_service "${NAMESPACE}")"
fi

if [ -z "${SERVICE}" ]; then
  echo "[오류] namespace ${NAMESPACE}에서 Elasticsearch HTTP 서비스를 찾지 못했습니다."
  exit 1
fi

start_port_forward "${NAMESPACE}" "${SERVICE}" "${LOCAL_PORT}" 9200 "/tmp/ai-search-nori-analyse-port-forward.log"
trap cleanup_port_forward EXIT

# JSON 문자열 최소 이스케이프
json_escape() {
  local s="$1"
  s="${s//\\/\\\\}"
  s="${s//\"/\\\"}"
  s="${s//$'\n'/\\n}"
  printf '%s' "${s}"
}

ESCAPED_TEXT="$(json_escape "${ANALYZE_TEXT}")"

#-----[{Nori _analyze 호출}]--------------
RESPONSE="$({
  curl -sS -u "${ES_USERNAME}:${ES_PASSWORD}" \
    -X POST "http://localhost:${LOCAL_PORT}/${ANALYZE_INDEX}/_analyze" \
    -H "Content-Type: application/json" \
    -d "{\"analyzer\":\"${ANALYZER_NAME}\",\"text\":\"${ESCAPED_TEXT}\"}"
} )"

#-----[{결과 출력}]--------------
if command -v jq >/dev/null 2>&1; then
  echo "${RESPONSE}" | jq .
else
  echo "${RESPONSE}"
fi

# 형태소 분석

