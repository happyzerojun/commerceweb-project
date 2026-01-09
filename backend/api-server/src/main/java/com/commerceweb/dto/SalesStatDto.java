package com.commerceweb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesStatDto {

    @JsonProperty("productName")
    private String productName;   // 상품 이름

    @JsonProperty("totalSales")
    private Long totalSales;      // 총 매출액

    @JsonProperty("totalQuantity")
    private Long totalQuantity;   // 총 팔린 개수

    @JsonProperty("averageRating")
    private Double averageRating; // 평균 별점
}