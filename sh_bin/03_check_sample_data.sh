#!/usr/bin/env bash
set -euo pipefail

# 스크립트 실행 위치와 상관없이 프로젝트 루트를 찾습니다.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# 생성된 샘플 데이터 건수/분포를 빠르게 확인합니다.
cd "${ROOT_DIR}"
python3 - <<'PY'
import json
from collections import Counter
from pathlib import Path

path = Path('src/main/resources/data/goods_template.json')
arr = json.loads(path.read_text(encoding='utf-8'))
print(f"[DATA] file={path}")
print(f"[DATA] count={len(arr)}")
if arr:
    print(f"[DATA] first={arr[0]['id']} | {arr[0]['productName']} | {arr[0]['category']}")
    print(f"[DATA] last={arr[-1]['id']} | {arr[-1]['productName']} | {arr[-1]['category']}")
    top = Counter(item.get('category', '') for item in arr).most_common(5)
    print('[DATA] top_categories=' + ', '.join(f"{k}:{v}" for k, v in top))
PY

# 여기도 신경 안씀
