package com.commerceweb.dto.Order;

import com.commerceweb.entity.Order;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderResponse {
    private Long id;
    private Long productId; // ✅ 상품 ID 필드 추가
    private String productName;
    private Integer quantity;
    private Long totalPrice;
    private String status;
    private LocalDateTime orderDate;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .productId(order.getProduct().getId()) // ✅ 엔티티에서 ID 추출
                .productName(order.getProduct().getName())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .build();
    }
}