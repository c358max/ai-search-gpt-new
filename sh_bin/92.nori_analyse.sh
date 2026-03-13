#!/usr/bin/env bash
set -euo pipefail

# POS(품사) 확인용 Nori analyze 스크립트
# 기본 모드: tokenizer (품사 정보 확인에 유리)
#
# 사용 예시
#   ./sh_bin/92.nori_analyse.sh "얇은피 만두"
#   ANALYZE_MODE=analyzer ANALYZER_NAME=ko_mall_search_analyzer ./sh_bin/92.nori_analyse.sh "얇은피 만두"
#   TOKENIZER_NAME=ko_nori_userdict_tokenizer TOKEN_FILTERS=lowercase,nori_part_of_speech ./sh_bin/92.nori_analyse.sh "얇은피 만두"

#-----[{기본값 설정}]--------------
ES_URL="${ES_URL:-http://localhost:9200}"
ES_USERNAME="${ES_USERNAME:-elastic}"
ANALYZE_INDEX="${ANALYZE_INDEX:-food-products-read}"
NAMESPACE="${NAMESPACE:-ai-search}"
SECRET_NAME="${SECRET_NAME:-ai-search-es-es-elastic-user}"

# analyzer | tokenizer
ANALYZE_MODE="${ANALYZE_MODE:-tokenizer}"

# analyzer 모드 파라미터
ANALYZER_NAME="${ANALYZER_NAME:-ko_mall_search_analyzer}"

# tokenizer 모드 파라미터
TOKENIZER_NAME="${TOKENIZER_NAME:-nori_tokenizer}"
TOKEN_FILTERS="${TOKEN_FILTERS:-}"
CHAR_FILTERS="${CHAR_FILTERS:-}"

# POS 관련 속성 (Analyze API attributes)
ATTRIBUTES="${ATTRIBUTES:-posType,leftPOS,rightPOS,morphemes,reading}"

# 첫 번째 인자로 분석할 텍스트를 받는다.
ANALYZE_TEXT="${1:-}"

#-----[{입력값 검증}]--------------
if [ -z "${ANALYZE_TEXT}" ]; then
  echo "[오류] 분석할 텍스트를 인자로 전달하세요."
  echo "[예시] ./sh_bin/92.nori_analyse.sh '얇은피 만두'"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[오류] curl 명령어를 찾을 수 없습니다."
  exit 1
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[오류] kubectl 명령어를 찾을 수 없습니다."
  exit 1
fi

if [ "${ANALYZE_MODE}" != "analyzer" ] && [ "${ANALYZE_MODE}" != "tokenizer" ]; then
  echo "[오류] ANALYZE_MODE는 analyzer 또는 tokenizer만 허용됩니다."
  exit 1
fi

#-----[{비밀번호 자동 조회}]--------------
ES_PASSWORD="$(kubectl get secret "${SECRET_NAME}" -n "${NAMESPACE}" -o go-template='{{.data.elastic | base64decode}}')"
if [ -z "${ES_PASSWORD}" ]; then
  echo "[오류] secret에서 Elasticsearch 비밀번호를 읽지 못했습니다."
  echo "[확인] namespace=${NAMESPACE}, secret=${SECRET_NAME}"
  exit 1
fi

# JSON 문자열 최소 이스케이프
json_escape() {
  local s="$1"
  s="${s//\\/\\\\}"
  s="${s//\"/\\\"}"
  s="${s//$'\n'/\\n}"
  printf '%s' "${s}"
}

trim() {
  local s="$1"
  # shellcheck disable=SC2001
  echo "${s}" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'
}

csv_to_json_array() {
  local csv="$1"
  if [ -z "$(trim "${csv}")" ]; then
    printf '[]'
    return 0
  fi
  local out="["
  local first=1
  local -a parts=()
  IFS=',' read -r -a parts <<< "${csv}" || true
  for raw in "${parts[@]}"; do
    local item
    item="$(trim "${raw}")"
    if [ -z "${item}" ]; then
      continue
    fi
    local esc
    esc="$(json_escape "${item}")"
    if [ ${first} -eq 0 ]; then
      out+=", "
    fi
    out+="\"${esc}\""
    first=0
  done
  out+="]"
  printf '%s' "${out}"
}

ESCAPED_TEXT="$(json_escape "${ANALYZE_TEXT}")"
ATTR_JSON="$(csv_to_json_array "${ATTRIBUTES}")"
FILTER_JSON="$(csv_to_json_array "${TOKEN_FILTERS}")"
CHAR_FILTER_JSON="$(csv_to_json_array "${CHAR_FILTERS}")"

#-----[{요청 JSON 구성}]--------------
if [ "${ANALYZE_MODE}" = "analyzer" ]; then
  ESCAPED_ANALYZER="$(json_escape "${ANALYZER_NAME}")"
  PAYLOAD="{\"analyzer\":\"${ESCAPED_ANALYZER}\",\"text\":\"${ESCAPED_TEXT}\",\"explain\":true,\"attributes\":${ATTR_JSON}}"
else
  ESCAPED_TOKENIZER="$(json_escape "${TOKENIZER_NAME}")"
  PAYLOAD="{\"tokenizer\":\"${ESCAPED_TOKENIZER}\",\"text\":\"${ESCAPED_TEXT}\",\"explain\":true,\"attributes\":${ATTR_JSON}"
  if [ "${CHAR_FILTER_JSON}" != "[]" ]; then
    PAYLOAD+=",\"char_filter\":${CHAR_FILTER_JSON}"
  fi
  if [ "${FILTER_JSON}" != "[]" ]; then
    PAYLOAD+=",\"filter\":${FILTER_JSON}"
  fi
  PAYLOAD+="}"
fi

echo "[INFO] mode=${ANALYZE_MODE}, index=${ANALYZE_INDEX}"
if [ "${ANALYZE_MODE}" = "analyzer" ]; then
  echo "[INFO] analyzer=${ANALYZER_NAME}"
else
  echo "[INFO] tokenizer=${TOKENIZER_NAME}, filter=${TOKEN_FILTERS:-<none>}"
fi

#-----[{Nori _analyze 호출}]--------------
RESPONSE="$({
  curl -sS -u "${ES_USERNAME}:${ES_PASSWORD}" \
    -X POST "${ES_URL}/${ANALYZE_INDEX}/_analyze" \
    -H "Content-Type: application/json" \
    -d "${PAYLOAD}"
})"

#-----[{결과 출력}]--------------
if command -v jq >/dev/null 2>&1; then
  echo "${RESPONSE}" | jq .

  echo
  echo "[POS 요약(token / leftPOS / leftPOS_ko / rightPOS / rightPOS_ko / posType)]"
  echo "${RESPONSE}" | jq -r '
    def pos_code:
      if . == null or . == "-" then "-"
      else (capture("^(?<code>[A-Z]+)").code? // .) end;
    def pos_ko($p):
      ($p | pos_code) as $c
      | if $c == "NNG" then "일반 명사"
        elif $c == "NNP" then "고유 명사"
        elif $c == "NNB" then "의존 명사"
        elif $c == "NR" then "수사"
        elif $c == "NP" then "대명사"
        elif $c == "VV" then "동사"
        elif $c == "VA" then "형용사"
        elif $c == "VX" then "보조 용언"
        elif $c == "VCP" then "긍정 지정사"
        elif $c == "VCN" then "부정 지정사"
        elif $c == "MM" then "관형사"
        elif $c == "MAG" then "일반 부사"
        elif $c == "MAJ" then "접속 부사"
        elif $c == "IC" then "감탄사"
        elif $c == "JKS" then "주격 조사"
        elif $c == "JKC" then "보격 조사"
        elif $c == "JKG" then "관형격 조사"
        elif $c == "JKO" then "목적격 조사"
        elif $c == "JKB" then "부사격 조사"
        elif $c == "JKV" then "호격 조사"
        elif $c == "JKQ" then "인용격 조사"
        elif $c == "JX" then "보조사"
        elif $c == "JC" then "접속 조사"
        elif $c == "EP" then "선어말 어미"
        elif $c == "EF" then "종결 어미"
        elif $c == "EC" then "연결 어미"
        elif $c == "ETN" then "명사형 전성 어미"
        elif $c == "ETM" then "관형형 전성 어미"
        elif $c == "XPN" then "체언 접두사"
        elif $c == "XSN" then "명사 파생 접미사"
        elif $c == "XSV" then "동사 파생 접미사"
        elif $c == "XSA" then "형용사 파생 접미사"
        elif $c == "XR" then "어근"
        elif $c == "SF" then "마침표/물음표/느낌표"
        elif $c == "SP" then "쉼표/가운뎃점/콜론/빗금"
        elif $c == "SS" then "괄호/따옴표"
        elif $c == "SE" then "줄임표"
        elif $c == "SO" then "붙임표/물결표"
        elif $c == "SW" then "기타 기호"
        elif $c == "SN" then "숫자"
        elif $c == "SL" then "외국어"
        elif $c == "SH" then "한자"
        elif $c == "UNA" then "분석 불능"
        elif $c == "NA" then "알 수 없음"
        else "기타"
        end;
    (.detail.tokenizer.tokens // .tokens // [])
    | .[]
    | [(.token // "-"), (.leftPOS // "-"), pos_ko(.leftPOS // "-"), (.rightPOS // "-"), pos_ko(.rightPOS // "-"), (.posType // "-")]
    | @tsv
  ' || true
else
  echo "${RESPONSE}"
  echo "[안내] jq를 설치하면 POS 요약 테이블이 함께 출력됩니다."
fi
