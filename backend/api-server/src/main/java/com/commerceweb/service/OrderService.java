package com.commerceweb.service;

import com.commerceweb.entity.Order;
import com.commerceweb.entity.Product;
import com.commerceweb.entity.User;
import com.commerceweb.repository.OrderRepository;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public Order createOrder(User user, Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        Order order = Order.builder()
                .user(user)
                .product(product)
                .quantity(quantity)
                .totalPrice((long) (product.getPrice() * quantity))
                .shippingAddress("기본 배송지") // 명시적 추가
                .status("PAID")               // 명시적 추가
                .build();

        return orderRepository.save(order);
    }

    public List<Order> getMyOrders(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    // ✅ 수정된 로직: 상태 변경이 아닌 실제 DB 삭제
    @Transactional
    public void cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 없습니다."));

        // 본인 주문인지 확인
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 주문만 취소할 수 있습니다.");
        }

        // 리뷰가 달린 주문은 reviews.order_id(NOT NULL FK)가 참조하고 있어 삭제하면 제약 위반(500)이 났다.
        // 소프트 취소로 바꾸기 전까지는 명확한 409로 막는다 (TS-07)
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new IllegalStateException("리뷰가 작성된 주문은 취소할 수 없습니다.");
        }

        // ✅ DB에서 실제 데이터 삭제
        orderRepository.delete(order);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 없습니다."));
        order.setStatus(status);
    }
}