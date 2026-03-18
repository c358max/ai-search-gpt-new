#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_DIR="${LOG_DIR:-/tmp/ai-search-model-logs}"
PID_DIR="${PID_DIR:-/tmp/ai-search-model-pids}"

ALL_MODELS=(
  "e5-small-ko-v2"
  "kure-v1"
  "bge-m3"
)

RUN_TESTS=true
RUN_INDEXING=true
TARGET_MODELS=()

usage() {
  cat <<'EOF'
[USAGE] ./sh_bin/40_deploy_dev_server.sh [--models <all|comma-separated>] [--skip-test] [--skip-index]

Examples
  ./sh_bin/40_deploy_dev_server.sh
  ./sh_bin/40_deploy_dev_server.sh --models e5-small-ko-v2
  ./sh_bin/40_deploy_dev_server.sh --models e5-small-ko-v2,kure-v1 --skip-test
  ./sh_bin/40_deploy_dev_server.sh --models all --skip-index
EOF
}

contains_model() {
  local target="$1"
  local item=""
  for item in "${ALL_MODELS[@]}"; do
    if [ "${item}" = "${target}" ]; then
      return 0
    fi
  done
  return 1
}

parse_models() {
  local raw="$1"
  local token=""

  if [ "${raw}" = "all" ]; then
    TARGET_MODELS=("${ALL_MODELS[@]}")
    return 0
  fi

  IFS=',' read -r -a TARGET_MODELS <<< "${raw}"
  for token in "${TARGET_MODELS[@]}"; do
    if ! contains_model "${token}"; then
      echo "[ERROR] unsupported model: ${token}"
      usage
      exit 1
    fi
  done
}

start_model_web() {
  local model="$1"
  local log_file="${LOG_DIR}/${model}.web.log"
  local pid_file="${PID_DIR}/${model}.pid"

  echo "[INFO] starting vector-only compare web server for ${model}"
  OPTIONAL_PROFILE="search-vector-only" "${SCRIPT_DIR}/21_run_model_web.sh" "${model}" > "${log_file}" 2>&1 &
  echo $! > "${pid_file}"
  echo "[INFO] ${model} pid=$(cat "${pid_file}") log=${log_file}"
  sleep 2

  if ! kill -0 "$(cat "${pid_file}")" >/dev/null 2>&1; then
    echo "[ERROR] ${model} compare web process exited early"
    tail -n 60 "${log_file}" || true
    exit 1
  fi
}

while [ $# -gt 0 ]; do
  case "$1" in
    --models)
      if [ $# -lt 2 ]; then
        echo "[ERROR] --models requires a value"
        usage
        exit 1
      fi
      parse_models "$2"
      shift 2
      ;;
    --skip-test)
      RUN_TESTS=false
      shift
      ;;
    --skip-index)
      RUN_INDEXING=false
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[ERROR] unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

if [ ${#TARGET_MODELS[@]} -eq 0 ]; then
  TARGET_MODELS=("${ALL_MODELS[@]}")
fi

mkdir -p "${LOG_DIR}" "${PID_DIR}"

echo "[INFO] deploy target models: ${TARGET_MODELS[*]}"
echo "[INFO] run tests: ${RUN_TESTS}"
echo "[INFO] run indexing: ${RUN_INDEXING}"

cd "${ROOT_DIR}"

if [ "${RUN_TESTS}" = true ]; then
  echo "[STEP] ./gradlew test"
  ./gradlew test
fi

echo "[STEP] stop existing model processes"
"${SCRIPT_DIR}/32_stop_all_models.sh"

if [ "${RUN_INDEXING}" = true ]; then
  for model in "${TARGET_MODELS[@]}"; do
    echo "[STEP] reindex ${model}"
    "${SCRIPT_DIR}/23_run_model_indexing_only.sh" "${model}"
  done
else
  echo "[STEP] skip indexing"
fi

for model in "${TARGET_MODELS[@]}"; do
  start_model_web "${model}"
done

echo "[STEP] check model status"
"${SCRIPT_DIR}/35_check_model_status.sh"

echo "[OK] deploy completed"
