#!/usr/bin/env bash
set -euo pipefail

#-----[{경로/기본 변수 초기화}]--------------
# 스크립트 위치와 프로젝트 루트를 먼저 계산한다.
# 이후 상대 경로(템플릿, 보조 스크립트, gradlew)를 안전하게 참조하기 위함이다.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# 실행 시 외부에서 덮어쓸 수 있는 기본 환경변수들.
# - NAMESPACE/STATEFULSET: Kubernetes 대상
# - ROLLOUT_TIMEOUT: Pod Ready 대기 최대 시간(초 단위, 예: 600s)
NAMESPACE="${NAMESPACE:-ai-search}"
STATEFULSET="${STATEFULSET:-ai-search-es-es-default}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-600s}"

# 사전 반영/ES 재시작에 필요한 스크립트 및 리소스 경로
CONFIGMAP_SCRIPT="${CONFIGMAP_SCRIPT:-${SCRIPT_DIR}/00_5_apply_user_dictionary_configmap.sh}"
DICT_MOUNT_PATH="${DICT_MOUNT_PATH:-/usr/share/elasticsearch/config/analysis/user_dict_ko.txt}"

# 재색인 실행 시 JVM truststore 설정
TRUSTSTORE_PATH="${TRUSTSTORE_PATH:-${HOME}/.ai-cert/djl-truststore.p12}"
TRUSTSTORE_PASSWORD="${TRUSTSTORE_PASSWORD:-changeit}"

#-----[{사전 점검: 필수 도구/파일}]--------------
# 실행 전에 반드시 필요한 항목을 점검한다.
# 조건을 만족하지 못하면 즉시 종료해, 중간 단계 실패로 인한 반쯤 적용 상태를 막는다.
if ! command -v kubectl >/dev/null 2>&1; then
  echo "[오류] kubectl 명령어를 찾을 수 없습니다."
  exit 1
fi

if [ ! -x "${ROOT_DIR}/gradlew" ]; then
  echo "[오류] ./gradlew 파일이 없거나 실행 권한이 없습니다."
  exit 1
fi

if [ ! -x "${CONFIGMAP_SCRIPT}" ]; then
  echo "[오류] ConfigMap 스크립트가 없거나 실행 권한이 없습니다: ${CONFIGMAP_SCRIPT}"
  exit 1
fi

if [ ! -f "${TRUSTSTORE_PATH}" ]; then
  echo "[오류] truststore 파일이 없습니다: ${TRUSTSTORE_PATH}"
  echo "[다음 작업] ./sh_bin/10_1_prepare_djl_truststore.sh 를 먼저 실행하세요."
  exit 1
fi

#-----[{실행 대상 컨텍스트 출력}]--------------
# 현재 kubectl context를 출력해, 잘못된 클러스터 대상으로 작업하는 실수를 줄인다.
CURRENT_CONTEXT="$(kubectl config current-context 2>/dev/null || true)"
if [ -n "${CURRENT_CONTEXT}" ]; then
  echo "[정보] 현재 kubectl context: ${CURRENT_CONTEXT}"
fi

#-----[{1단계: 사용자 사전 ConfigMap 반영}]--------------
# user_dict_ko.txt를 ConfigMap으로 반영한다.
# 이 단계는 실제 ES Pod에서 파일 마운트를 구성하기 위한 선행 조건이다.
echo "[1/3] 사용자 사전 ConfigMap 반영"
"${CONFIGMAP_SCRIPT}"

#-----[{2단계: ES 재시작 + Ready 대기}]--------------
# 핵심 의도:
#
# 전제:
# - 00_6 단계에서 user-dict 마운트가 포함된 Elasticsearch CR이 이미 적용되어 있어야 한다.
# - 이 스크립트는 "사전 파일 갱신 후 재시작/재색인" 반복 운영만 담당한다.
echo "[2/3] Elasticsearch 재시작 후 준비 상태 대기"
set -x
kubectl rollout restart "statefulset/${STATEFULSET}" -n "${NAMESPACE}"
set +x

# "600s" -> "600" 으로 변환 후 숫자 유효성 검증
timeout_sec="${ROLLOUT_TIMEOUT%s}"
if [ -z "${timeout_sec}" ] || ! echo "${timeout_sec}" | grep -Eq '^[0-9]+$'; then
  echo "[오류] ROLLOUT_TIMEOUT 값이 올바르지 않습니다: ${ROLLOUT_TIMEOUT} (예: 600s)"
  exit 1
fi

# 대상 StatefulSet의 Pod들이 모두 Running + Ready=true가 될 때까지 폴링
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

# 마운트 파일 존재를 직접 확인한다.
# 이 검증이 없으면 이후 인덱스 생성 단계에서 user_dictionary 파일 읽기 오류가 발생할 수 있다.
POD_NAME="$(kubectl get pods -n "${NAMESPACE}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' | awk -v prefix="${STATEFULSET}-" '$1 ~ "^" prefix {print; exit}')"
if [ -z "${POD_NAME}" ]; then
  echo "[오류] StatefulSet ${STATEFULSET}에서 Elasticsearch Pod 이름을 찾지 못했습니다."
  exit 1
fi

if ! kubectl exec -n "${NAMESPACE}" "pod/${POD_NAME}" -- test -f "${DICT_MOUNT_PATH}"; then
  echo "[오류] Pod 내부에 사용자 사전 파일이 마운트되지 않았습니다: ${DICT_MOUNT_PATH}"
  echo "[힌트] 초기 구성 단계(00_6)에서 user-dict 마운트가 포함된 CR이 적용되었는지 확인하세요."
  exit 1
fi

#-----[{3단계: 인덱스 롤아웃 실행}]--------------
# indexing 프로파일로 애플리케이션을 실행해
# - 신규 인덱스 생성
# - 전체 재색인
# - read alias 전환
# 을 한 번에 수행한다.
echo "[3/3] 인덱스 롤아웃 실행(재색인 + alias 전환)"
(
  cd "${ROOT_DIR}"
  ./gradlew bootRun \
    --args='--spring.profiles.active=indexing' \
    -Djavax.net.ssl.trustStore="${TRUSTSTORE_PATH}" \
    -Djavax.net.ssl.trustStorePassword="${TRUSTSTORE_PASSWORD}"
)

#-----[{종료 안내}]--------------
# 실제 토큰화 반영 여부는 검증 스크립트로 분리했다.
echo "[완료] 사용자 사전 재적용 작업이 완료되었습니다. (검증은 분리 실행)"
echo "[다음 작업] ./sh_bin/05_2_verifyUserDictionary.sh 실행"

# 사용자 정의사전 리로드