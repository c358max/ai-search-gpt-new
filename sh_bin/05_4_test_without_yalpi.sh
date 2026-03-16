#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "[시나리오] without-yalpi"

ACTIVE_DICT_SOURCE_PATH="${ROOT_DIR}/.local-nas/userdict/user_dict_ko.without-yalpi.txt" \
  "${SCRIPT_DIR}/05_1_reloadUserDictionary.sh"

ANALYZE_TEXT="얄피 만두" \
ANALYZE_EXPECTED_TOKEN="" \
ANALYZE_EXPECTED_TOKENS="얄,피,만두,교자" \
ANALYZE_UNEXPECTED_TOKENS="얄피,얇은피" \
  "${SCRIPT_DIR}/05_2_verifyUserDictionary.sh"

echo "[완료] without-yalpi 시나리오 검증이 완료되었습니다."
