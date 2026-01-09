package com.commerceweb.dto.product;

import com.commerceweb.entity.Product;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private Double price;
    private String description;
    private String imageUrl;
    private String category;

    private Long viewCount;
    private Double averageRating;
    private Long ratingCount;

    public static ProductResponse from(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .category(p.getCategory())
                .viewCount(p.getViewCount())
                .averageRating(p.getAverageRating())
                .ratingCount(p.getRatingCount())
                .build();
    }
}
