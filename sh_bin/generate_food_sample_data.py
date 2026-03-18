#!/usr/bin/env python3
import json
from pathlib import Path

WEIGHTS = ["80g", "120g", "200g", "350g", "500g", "1kg"]

SNACK_PRODUCTS = [
    "사과 과일칩",
    "바나나 과일칩",
    "딸기 과일칩",
    "망고 과일칩",
    "배 과일칩",
    "고구마칩",
    "현미쌀과자",
    "유기농 과일퓨레",
    "아이용 동결건조 과일",
    "무첨가 과일스틱",
]

OTHER_CATEGORY_PRODUCTS = {
    "수산물": ["손질고등어", "훈제연어", "새우살", "오징어채", "명란젓"],
    "정육/가공육": ["닭가슴살", "불고기", "돈까스", "훈제오리", "한입스테이크"],
    "밀키트": ["된장찌개 밀키트", "부대찌개 밀키트", "파스타 밀키트", "샤브샤브 밀키트", "감바스 밀키트"],
    "건강식품": ["프로틴바", "오트밀", "저당쿠키", "곤약젤리", "단백질쉐이크"],
    "비건식품": ["비건 만두", "비건 패티", "두부스테이크", "귀리음료", "식물성 요거트"],
}


def build_snack_item(i: int):
    base = SNACK_PRODUCTS[(i - 1) % len(SNACK_PRODUCTS)]
    weight = WEIGHTS[(i - 1) % len(WEIGHTS)]
    category = "과일/간식" if i % 2 == 1 else "유아식/아이간식"
    product_name = f"아이들 간식 {base} {weight}"
    description = (
        f"{product_name} 상품입니다. 아이들 간식으로 좋은 과일칩/쌀과자 계열 제품입니다. "
        f"추천 키워드: 아이들 간식, 과일칩, {category}, {base}."
    )
    return {"id": str(i), "productName": product_name, "category": category, "description": description}


def build_other_item(i: int):
    categories = list(OTHER_CATEGORY_PRODUCTS.keys())
    category = categories[(i - 71) % len(categories)]
    names = OTHER_CATEGORY_PRODUCTS[category]
    base = names[(i - 71) % len(names)]
    weight = WEIGHTS[(i - 1) % len(WEIGHTS)]
    product_name = f"담백한 {base} {weight}"
    description = (
        f"{product_name} 상품입니다. {category} 카테고리의 일반 식사용 제품이며 "
        f"단백질과 식사 균형에 초점을 맞췄습니다."
    )
    return {"id": str(i), "productName": product_name, "category": category, "description": description}


def main():
    total = 120
    data = []
    for i in range(1, total + 1):
        if i <= 70:
            data.append(build_snack_item(i))
        else:
            data.append(build_other_item(i))

    output = Path("src/main/resources/data/goods_template.json")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"generated deterministic {total} items -> {output}")


if __name__ == "__main__":
    main()
