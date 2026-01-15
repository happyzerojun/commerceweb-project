package com.commerceweb.dto.Order;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private Long productId;     // 상품 ID (필수)
    private Integer quantity;   // 주문 수량 (필수)
}