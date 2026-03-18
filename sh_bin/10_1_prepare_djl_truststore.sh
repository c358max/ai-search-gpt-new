#!/usr/bin/env bash
set -euo pipefail

# DJL 모델 다운로드용 HTTPS 인증서를 truststore에 미리 등록합니다.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# TRUSTSTORE_PATH는 "여러 인증서를 담는 truststore(.p12) 파일 경로"입니다.
# 즉, 개별 인증서 파일 경로가 아니라 인증서 저장소 파일 경로입니다.
TRUSTSTORE_PATH="${HOME}/.ai-cert/djl-truststore.p12"
echo "인증서 경로 : " + $TRUSTSTORE_PATH

# TRUSTSTORE_PASSWORD는 truststore 파일을 열 때 쓰는 비밀번호입니다.
# (인증서마다 다른 비밀번호를 두는 개념이 아니라, 저장소 파일 비밀번호 1개를 사용)
TRUSTSTORE_PASSWORD="${AI_SEARCH_TRUSTSTORE_PASSWORD:-changeit}"
# 인증서를 가져올 대상 호스트 목록입니다. (DJL가 실제로 접근하는 도메인)
DJL_HOSTS=("djl.ai" "resources.djl.ai" "mlrepo.djl.ai")

# openssl은 원격 서버 인증서를 내려받을 때 사용합니다.
if ! command -v openssl >/dev/null 2>&1; then
  echo "[ERROR] openssl not found"
  exit 1
fi

# macOS라면 Java 21 경로를 자동으로 찾아 JAVA_HOME에 설정합니다.
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  if JAVA21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null); then
    export JAVA_HOME="${JAVA21_HOME}"
    echo "[INFO] JAVA_HOME set to Java 21 (${JAVA_HOME})"
  fi
fi

# keytool은 truststore 파일을 만들고 인증서를 넣는 도구입니다.
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/keytool" ]; then
  echo "[ERROR] keytool not found (JAVA_HOME is required)"
  exit 1
fi


echo "[INFO] preparing DJL truststore"
# truststore 저장 폴더를 만들고, 기존 파일이 있으면 새로 생성하기 위해 삭제합니다.
mkdir -p "$(dirname "${TRUSTSTORE_PATH}")"
rm -f "${TRUSTSTORE_PATH}"

# 호스트별로 인증서를 내려받아 truststore에 등록합니다.
for host in "${DJL_HOSTS[@]}"; do
  # cert_tmp: 인증서 체인을 임시로 저장하는 파일
  # split_prefix: 인증서 분리 파일의 접두사
  cert_tmp=$(mktemp)
  split_prefix=$(mktemp -u)

  # 원격 서버와 TLS 핸드셰이크를 수행하고 인증서 체인만 추출합니다.
  openssl s_client -showcerts -servername "${host}" -connect "${host}:443" </dev/null 2>/dev/null \
    | awk '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/' > "${cert_tmp}"

  # 인증서를 하나도 못 받으면 해당 호스트 처리 실패로 종료합니다.
  if [ ! -s "${cert_tmp}" ]; then
    echo "[ERROR] failed to fetch certificates from ${host}"
    exit 1
  fi

  # 체인 파일(여러 인증서 포함)을 인증서 1개당 1개 PEM 파일로 분리합니다.
  awk -v prefix="${split_prefix}" '
    /-----BEGIN CERTIFICATE-----/ {n++; file=sprintf("%s.%02d.pem", prefix, n)}
    n > 0 {print > file}
    /-----END CERTIFICATE-----/ {close(file)}
  ' "${cert_tmp}"

  # 같은 호스트에서 여러 인증서(서버/중간/루트)가 나올 수 있어서 순번을 둡니다.
  idx=0
  for cert_file in "${split_prefix}".*.pem; do
    # 파일이 비어있으면 건너뜁니다.
    if [ ! -s "${cert_file}" ]; then
      continue
    fi

    # truststore 안에서 각 인증서를 구분할 고유 이름(alias)입니다.
    # 예: djl.ai-0, djl.ai-1
    alias="${host}-${idx}"

    # PEM 인증서를 PKCS12 truststore에 추가합니다.
    # -noprompt: "신뢰하시겠습니까?" 질문 없이 자동 추가
    "${JAVA_HOME}/bin/keytool" -importcert -noprompt \
      -storetype PKCS12 \
      -keystore "${TRUSTSTORE_PATH}" \
      -storepass "${TRUSTSTORE_PASSWORD}" \
      -alias "${alias}" \
      -file "${cert_file}" >/dev/null

    # 다음 인증서 alias를 위해 번호를 1 증가시킵니다.
    idx=$((idx + 1))
  done

  # 분리/등록된 인증서가 0개면 비정상 상태이므로 종료합니다.
  if [ "${idx}" -eq 0 ]; then
    echo "[ERROR] no certificates extracted for ${host}"
    exit 1
  fi

  # 현재 호스트에서 truststore에 등록된 인증서 개수를 출력합니다.
  echo "[INFO] ${host} certificates imported: ${idx}"

  # 이번 호스트 처리에 사용한 임시 파일들을 정리합니다.
  rm -f "${cert_tmp}" "${split_prefix}".*
done

# 모든 호스트 인증서 등록이 끝나면 생성 경로를 출력합니다.
echo "[OK] truststore created: ${TRUSTSTORE_PATH}"

# URL base를 인증서로