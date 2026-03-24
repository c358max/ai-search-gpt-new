package com.example.aisearch.service.indexing.bootstrap.ingest;

import com.example.aisearch.model.FoodProduct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FoodProductDocumentMapper {

    public IndexDocument toIndexDocument(FoodProduct food, List<Float> embedding) {
        Map<String, Object> doc = new HashMap<>();
        List<Integer> lev3CategoryIds = parseIntegers(food.getLev3CategoryId());
        doc.put("id", food.getId());
        doc.put("goods_id", food.getGoodsId());
        doc.put("goods_name", food.getGoodsName());
        doc.put("goods_full_name", food.getGoodsFullName());
        doc.put("promotion_name", food.getPromotionName());
        doc.put("thumbnail_path", food.getThumbnailPath());
        doc.put("supplier_id", food.getSupplierId());
        doc.put("brand_id", food.getBrandId());
        doc.put("search_keyword", food.getSearchKeyword());
        doc.put("brand_name", food.getBrandName());
        doc.put("lev2_category_id", food.getLev2CategoryId());
        doc.put("lev2_category_id_name", food.getLev2CategoryIdName());
        doc.put("lev3_category_id", lev3CategoryIds);
        doc.put("primary_lev3_category_id", lev3CategoryIds.isEmpty() ? null : lev3CategoryIds.get(0));
        doc.put("lev3_category_id_name", food.getLev3CategoryIdName());
        doc.put("main_lev1_category_id", food.getMainLev1CategoryId());
        doc.put("main_lev1_category_id_name", food.getMainLev1CategoryIdName());
        doc.put("mall_id_name", food.getMallIdName());
        doc.put("delivery_type_id_name", food.getDeliveryTypeIdName());
        doc.put("benefit_type_id_name", food.getBenefitTypeIdName());
        doc.put("certification_type_id_name", food.getCertificationTypeIdName());
        doc.put("storage_method_name", food.getStorageMethodName());
        doc.put("recommended_price", food.getRecommendedPrice());
        doc.put("sale_price", food.getSalePrice());
        doc.put("discount_rate", food.getDiscountRate());
        doc.put("popularity_score", food.getPopularityScore());
        doc.put("satisfaction_score", food.getSatisfactionScore());
        doc.put("satisfaction_count", food.getSatisfactionCount());
        doc.put("is_best_goods", food.getBestGoods());
        doc.put("is_recommended_goods", food.getRecommendedGoods());
        doc.put("is_new_goods", food.getNewGoods());
        doc.put("product_vector", embedding);
        return new IndexDocument(food.getId(), doc);
    }

    private List<Integer> parseIntegers(String rawValue) {
        List<Integer> values = new ArrayList<>();
        if (rawValue == null || rawValue.isBlank()) {
            return values;
        }
        for (String token : rawValue.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            values.add(Integer.valueOf(trimmed));
        }
        return values;
    }
}
