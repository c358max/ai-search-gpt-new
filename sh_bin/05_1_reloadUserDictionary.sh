#!/usr/bin/env bash
set -euo pipefail

#-----[{경로/기본 변수 초기화}]--------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

NAMESPACE="${NAMESPACE:-ai-search}"
STATEFULSET="${STATEFULSET:-ai-search-es-es-default}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-600s}"

# 현재 구조는 ConfigMap이 아니라 PV/PVC 기반 active 파일 마운트다.
ACTIVE_DICT_SOURCE_PATH="${ACTIVE_DICT_SOURCE_PATH:-${ROOT_DIR}/.local-nas/userdict/user_dict_ko.with-yalpi.txt}"
ACTIVE_DICT_PATH="${ACTIVE_DICT_PATH:-${ROOT_DIR}/.local-nas/userdict/user_dict_ko.txt}"
DICT_MOUNT_PATH="${DICT_MOUNT_PATH:-/usr/share/elasticsearch/config/userdict/user_dict_ko.txt}"

TRUSTSTORE_PATH="${TRUSTSTORE_PATH:-${HOME}/.ai-cert/djl-truststore.p12}"
TRUSTSTORE_PASSWORD="${TRUSTSTORE_PASSWORD:-changeit}"

#-----[{사전 점검}]--------------
if ! command -v kubectl >/dev/null 2>&1; then
  echo "[오류] kubectl 명령어를 찾을 수 없습니다."
  exit 1
fi

if [ ! -x "${ROOT_DIR}/gradlew" ]; then
  echo "[오류] ./gradlew 파일이 없거나 실행 권한이 없습니다."
  exit 1
fi

if [ ! -f "${ACTIVE_DICT_SOURCE_PATH}" ]; then
  echo "[오류] source user dictionary 파일이 없습니다: ${ACTIVE_DICT_SOURCE_PATH}"
  exit 1
fi

if [ ! -f "${TRUSTSTORE_PATH}" ]; then
  echo "[오류] truststore 파일이 없습니다: ${TRUSTSTORE_PATH}"
  echo "[다음 작업] ./sh_bin/10_1_prepare_djl_truststore.sh 를 먼저 실행하세요."
  exit 1
fi

CURRENT_CONTEXT="$(kubectl config current-context 2>/dev/null || true)"
if [ -n "${CURRENT_CONTEXT}" ]; then
  echo "[정보] 현재 kubectl context: ${CURRENT_CONTEXT}"
fi

echo "[1/3] active 사전 파일 갱신"
cp "${ACTIVE_DICT_SOURCE_PATH}" "${ACTIVE_DICT_PATH}"

echo "       source=${ACTIVE_DICT_SOURCE_PATH}"
echo "       active=${ACTIVE_DICT_PATH}"
if command -v shasum >/dev/null 2>&1; then
  echo "       sha1=$(shasum "${ACTIVE_DICT_PATH}" | awk '{print $1}')"
fi

#-----[{2단계: ES 상태 + 마운트 확인}]--------------
echo "[2/3] Elasticsearch 준비 상태 및 마운트 확인"
timeout_sec="${ROLLOUT_TIMEOUT%s}"
if [ -z "${timeout_sec}" ] || ! echo "${timeout_sec}" | grep -Eq '^[0-9]+$'; then
  echo "[오류] ROLLOUT_TIMEOUT 값이 올바르지 않습니다: ${ROLLOUT_TIMEOUT} (예: 600s)"
  exit 1
fi

start_ts=$(date +%s)
while true; do
  pod_lines="$(kubectl get pods -n "${NAMESPACE}" -o jsonpath='{range .items[*]}{.metadata.name} {.status.phase} {.status.containerStatuses[0].ready}{"\n"}{end}' \
    | awk -v prefix="${STATEFULSET}-" '$1 ~ "^" prefix {print}')"

  total="$(echo "${pod_lines}" | sed '/^$/d' | wc -l | tr -d ' ')"
  ready="0"
  if [ "${total}" -gt 0 ]; then
    ready="$(echo "${pod_lines}" | awk '$2=="Running" && $3=="true" {c++} END{print c+0}')"
  fi

  echo "[정보] statefulset=${STATEFULSET} 준비된 Pod=${ready}/${total}"
  if [ "${total}" -gt 0 ] && [ "${ready}" -eq "${total}" ]; then
    break
  fi

  now_ts=$(date +%s)
  if [ $((now_ts - start_ts)) -ge "${timeout_sec}" ]; then
    echo "[오류] StatefulSet Pod 준비 대기 시간 초과 (${ROLLOUT_TIMEOUT})"
    kubectl get pods -n "${NAMESPACE}" | awk -v prefix="${STATEFULSET}-" 'NR==1 || $1 ~ "^" prefix {print}'
    exit 1
  fi
  sleep 5
done

POD_NAME="$(kubectl get pods -n "${NAMESPACE}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' | awk -v prefix="${STATEFULSET}-" '$1 ~ "^" prefix {print; exit}')"
if [ -z "${POD_NAME}" ]; then
  echo "[오류] StatefulSet ${STATEFULSET}에서 Elasticsearch Pod 이름을 찾지 못했습니다."
  exit 1
fi

if ! kubectl exec -n "${NAMESPACE}" "pod/${POD_NAME}" -- test -f "${DICT_MOUNT_PATH}"; then
  echo "[오류] Pod 내부에 사용자 사전 파일이 마운트되지 않았습니다: ${DICT_MOUNT_PATH}"
  echo "[힌트] 00_6 단계에서 PV/PVC 기반 userdict mount가 포함된 CR이 적용되었는지 확인하세요."
  exit 1
fi

echo "[정보] Pod 내부 마운트 파일 확인 완료: ${DICT_MOUNT_PATH}"

#-----[{3단계: 인덱스 롤아웃 실행}]--------------
echo "[3/3] 인덱스 롤아웃 실행(재색인 + alias 전환)"
(
  cd "${ROOT_DIR}"
  ./gradlew bootRun \
    --args='--spring.profiles.active=indexing' \
    -Djavax.net.ssl.trustStore="${TRUSTSTORE_PATH}" \
    -Djavax.net.ssl.trustStorePassword="${TRUSTSTORE_PASSWORD}"
)

echo "[완료] 사용자 사전 재적용 작업이 완료되었습니다. (PV/PVC 기반, 검증은 분리 실행)"
echo "[다음 작업] ./sh_bin/05_2_verifyUserDictionary.sh 실행"

# 사용자 정의사전 리로드