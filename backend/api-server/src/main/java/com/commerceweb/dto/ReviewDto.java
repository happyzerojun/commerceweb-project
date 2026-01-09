package com.commerceweb.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {
    private Long id;
    private Long productId;
    private String productName; // ✅ 추가: 화면에 어떤 상품 리뷰인지 보여주기 위함
    private Long orderId;
    private Long userId;
    private String userName;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
}