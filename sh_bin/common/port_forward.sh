#!/usr/bin/env bash

# 공통 kubectl port-forward 유틸리티
# source 해서 사용한다.

find_es_http_service() {
  local namespace="$1"
  kubectl get svc -n "${namespace}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' \
    | awk '/-es-http$/ {print; exit}'
}

is_local_port_in_use() {
  local local_port="$1"

  if command -v lsof >/dev/null 2>&1; then
    lsof -iTCP:"${local_port}" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi

  if command -v nc >/dev/null 2>&1; then
    nc -z localhost "${local_port}" >/dev/null 2>&1
    return $?
  fi

  return 1
}

start_port_forward() {
  local namespace="$1"
  local service="$2"
  local local_port="$3"
  local remote_port="${4:-9200}"
  local log_file="$5"

  if [ -z "${service}" ]; then
    echo "[ERROR] service name is empty"
    return 1
  fi

  if is_local_port_in_use "${local_port}"; then
    echo "[INFO] local port ${local_port} is already open. reusing existing port-forward or listener"
    PF_PID=""
    return 0
  fi

  echo "[INFO] starting temporary port-forward"
  kubectl port-forward -n "${namespace}" "service/${service}" "${local_port}:${remote_port}" >"${log_file}" 2>&1 &
  PF_PID=$!

  local waited=0
  while [ "${waited}" -lt 10 ]; do
    if ! kill -0 "${PF_PID}" >/dev/null 2>&1; then
      echo "[ERROR] port-forward process exited early"
      if [ -f "${log_file}" ]; then
        cat "${log_file}"
      fi
      return 1
    fi

    if is_local_port_in_use "${local_port}"; then
      return 0
    fi

    sleep 1
    waited=$((waited + 1))
  done

  echo "[ERROR] port-forward did not become ready on localhost:${local_port}"
  if [ -f "${log_file}" ]; then
    cat "${log_file}"
  fi
  return 1
}

cleanup_port_forward() {
  if [ -n "${PF_PID:-}" ]; then
    kill "${PF_PID}" >/dev/null 2>&1 || true
  fi
}
