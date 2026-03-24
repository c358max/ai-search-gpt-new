package com.example.aisearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FoodProduct {

    // 상품 ID
    private String id;
    // 상품명
    @JsonProperty("goods_name")
    private String goodsName;
    // 전체 상품명
    @JsonProperty("goods_full_name")
    private String goodsFullName;
    // 내부 상품 ID
    @JsonProperty("goods_id")
    private String goodsId;
    // 프로모션명
    @JsonProperty("promotion_name")
    private String promotionName;
    // 썸네일 경로
    @JsonProperty("thumbnail_path")
    private String thumbnailPath;
    // 공급사 ID
    @JsonProperty("supplier_id")
    private String supplierId;
    // 브랜드 ID
    @JsonProperty("brand_id")
    private String brandId;
    // 검색 키워드
    @JsonProperty("search_keyword")
    private String searchKeyword;
    // 브랜드명
    @JsonProperty("brand_name")
    private String brandName;
    // 2레벨 카테고리 ID
    @JsonProperty("lev2_category_id")
    private String lev2CategoryId;
    // 2레벨 카테고리명
    @JsonProperty("lev2_category_id_name")
    private String lev2CategoryIdName;
    // 3레벨 카테고리 ID
    @JsonProperty("lev3_category_id")
    private String lev3CategoryId;
    // 3레벨 카테고리명
    @JsonProperty("lev3_category_id_name")
    private String lev3CategoryIdName;
    // 메인 1레벨 카테고리 ID
    @JsonProperty("main_lev1_category_id")
    private String mainLev1CategoryId;
    // 메인 1레벨 카테고리명
    @JsonProperty("main_lev1_category_id_name")
    private String mainLev1CategoryIdName;
    // 몰 구분명
    @JsonProperty("mall_id_name")
    private String mallIdName;
    // 배송 타입명
    @JsonProperty("delivery_type_id_name")
    private String deliveryTypeIdName;
    // 혜택 타입명
    @JsonProperty("benefit_type_id_name")
    private String benefitTypeIdName;
    // 인증 타입명
    @JsonProperty("certification_type_id_name")
    private String certificationTypeIdName;
    // 보관 방법명
    @JsonProperty("storage_method_name")
    private String storageMethodName;
    // 정상가
    @JsonProperty("recommended_price")
    private Integer recommendedPrice;
    // 판매가
    @JsonProperty("sale_price")
    private Integer salePrice;
    // 할인율
    @JsonProperty("discount_rate")
    private Integer discountRate;
    // 인기도 점수
    @JsonProperty("popularity_score")
    private Double popularityScore;
    // 만족도 점수
    @JsonProperty("satisfaction_score")
    private Double satisfactionScore;
    // 만족도 건수
    @JsonProperty("satisfaction_count")
    private Integer satisfactionCount;
    // 베스트 상품 여부
    @JsonProperty("is_best_goods")
    private Boolean bestGoods;
    // 추천 상품 여부
    @JsonProperty("is_recommended_goods")
    private Boolean recommendedGoods;
    // 신상품 여부
    @JsonProperty("is_new_goods")
    private Boolean newGoods;

    public FoodProduct() {
    }

    public FoodProduct(
            String id,
            String goodsName,
            String goodsFullName,
            String goodsId,
            String promotionName,
            String thumbnailPath,
            String supplierId,
            String brandId,
            String searchKeyword,
            String brandName,
            String lev2CategoryId,
            String lev2CategoryIdName,
            String lev3CategoryId,
            String lev3CategoryIdName,
            String mainLev1CategoryId,
            String mainLev1CategoryIdName,
            String mallIdName,
            String deliveryTypeIdName,
            String benefitTypeIdName,
            String certificationTypeIdName,
            String storageMethodName,
            Integer recommendedPrice,
            Integer salePrice,
            Integer discountRate,
            Double popularityScore,
            Double satisfactionScore,
            Integer satisfactionCount,
            Boolean bestGoods,
            Boolean recommendedGoods,
            Boolean newGoods
    ) {
        this.id = id;
        this.goodsName = goodsName;
        this.goodsFullName = goodsFullName;
        this.goodsId = goodsId;
        this.promotionName = promotionName;
        this.thumbnailPath = thumbnailPath;
        this.supplierId = supplierId;
        this.brandId = brandId;
        this.searchKeyword = searchKeyword;
        this.brandName = brandName;
        this.lev2CategoryId = lev2CategoryId;
        this.lev2CategoryIdName = lev2CategoryIdName;
        this.lev3CategoryId = lev3CategoryId;
        this.lev3CategoryIdName = lev3CategoryIdName;
        this.mainLev1CategoryId = mainLev1CategoryId;
        this.mainLev1CategoryIdName = mainLev1CategoryIdName;
        this.mallIdName = mallIdName;
        this.deliveryTypeIdName = deliveryTypeIdName;
        this.benefitTypeIdName = benefitTypeIdName;
        this.certificationTypeIdName = certificationTypeIdName;
        this.storageMethodName = storageMethodName;
        this.recommendedPrice = recommendedPrice;
        this.salePrice = salePrice;
        this.discountRate = discountRate;
        this.popularityScore = popularityScore;
        this.satisfactionScore = satisfactionScore;
        this.satisfactionCount = satisfactionCount;
        this.bestGoods = bestGoods;
        this.recommendedGoods = recommendedGoods;
        this.newGoods = newGoods;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsFullName() {
        return goodsFullName;
    }

    public void setGoodsFullName(String goodsFullName) {
        this.goodsFullName = goodsFullName;
    }

    public String getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getBrandId() {
        return brandId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getLev2CategoryId() {
        return lev2CategoryId;
    }

    public void setLev2CategoryId(String lev2CategoryId) {
        this.lev2CategoryId = lev2CategoryId;
    }

    public String getLev2CategoryIdName() {
        return lev2CategoryIdName;
    }

    public void setLev2CategoryIdName(String lev2CategoryIdName) {
        this.lev2CategoryIdName = lev2CategoryIdName;
    }

    public String getLev3CategoryId() {
        return lev3CategoryId;
    }

    public void setLev3CategoryId(String lev3CategoryId) {
        this.lev3CategoryId = lev3CategoryId;
    }

    public String getLev3CategoryIdName() {
        return lev3CategoryIdName;
    }

    public void setLev3CategoryIdName(String lev3CategoryIdName) {
        this.lev3CategoryIdName = lev3CategoryIdName;
    }

    public String getMainLev1CategoryId() {
        return mainLev1CategoryId;
    }

    public void setMainLev1CategoryId(String mainLev1CategoryId) {
        this.mainLev1CategoryId = mainLev1CategoryId;
    }

    public String getMainLev1CategoryIdName() {
        return mainLev1CategoryIdName;
    }

    public void setMainLev1CategoryIdName(String mainLev1CategoryIdName) {
        this.mainLev1CategoryIdName = mainLev1CategoryIdName;
    }

    public String getMallIdName() {
        return mallIdName;
    }

    public void setMallIdName(String mallIdName) {
        this.mallIdName = mallIdName;
    }

    public String getDeliveryTypeIdName() {
        return deliveryTypeIdName;
    }

    public void setDeliveryTypeIdName(String deliveryTypeIdName) {
        this.deliveryTypeIdName = deliveryTypeIdName;
    }

    public String getBenefitTypeIdName() {
        return benefitTypeIdName;
    }

    public void setBenefitTypeIdName(String benefitTypeIdName) {
        this.benefitTypeIdName = benefitTypeIdName;
    }

    public String getCertificationTypeIdName() {
        return certificationTypeIdName;
    }

    public void setCertificationTypeIdName(String certificationTypeIdName) {
        this.certificationTypeIdName = certificationTypeIdName;
    }

    public String getStorageMethodName() {
        return storageMethodName;
    }

    public void setStorageMethodName(String storageMethodName) {
        this.storageMethodName = storageMethodName;
    }

    public Integer getRecommendedPrice() {
        return recommendedPrice;
    }

    public void setRecommendedPrice(Integer recommendedPrice) {
        this.recommendedPrice = recommendedPrice;
    }

    public Integer getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Integer salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(Integer discountRate) {
        this.discountRate = discountRate;
    }

    public Double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(Double popularityScore) {
        this.popularityScore = popularityScore;
    }

    public Double getSatisfactionScore() {
        return satisfactionScore;
    }

    public void setSatisfactionScore(Double satisfactionScore) {
        this.satisfactionScore = satisfactionScore;
    }

    public Integer getSatisfactionCount() {
        return satisfactionCount;
    }

    public void setSatisfactionCount(Integer satisfactionCount) {
        this.satisfactionCount = satisfactionCount;
    }

    public Boolean getBestGoods() {
        return bestGoods;
    }

    public void setBestGoods(Boolean bestGoods) {
        this.bestGoods = bestGoods;
    }

    public Boolean getRecommendedGoods() {
        return recommendedGoods;
    }

    public void setRecommendedGoods(Boolean recommendedGoods) {
        this.recommendedGoods = recommendedGoods;
    }

    public Boolean getNewGoods() {
        return newGoods;
    }

    public void setNewGoods(Boolean newGoods) {
        this.newGoods = newGoods;
    }

    public String toEmbeddingText() {
        // goods_template 기준 주요 검색 필드를 합쳐 임베딩 텍스트로 사용한다.
        return String.join(" ",
                safe(goodsName),
                safe(goodsFullName),
                safe(promotionName),
                safe(lev2CategoryIdName),
                safe(lev3CategoryIdName),
                safe(searchKeyword),
                safe(brandName),
                safe(storageMethodName)
        ).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
