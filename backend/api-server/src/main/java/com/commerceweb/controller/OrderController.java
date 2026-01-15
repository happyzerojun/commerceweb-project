package com.commerceweb.controller;

import com.commerceweb.dto.Order.CreateOrderRequest;                  // ✅ 개선: Map 대신 명시적 DTO 사용
import com.commerceweb.dto.Order.OrderResponse;
import com.commerceweb.entity.Order;
import com.commerceweb.entity.User;
import com.commerceweb.service.OrderService;
import com.commerceweb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails; // ✅ 개선: Spring Security 표준 인터페이스 사용
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * 주문 생성 API (개선됨)
     * - 인증 정보: UserDetails (표준 방식)
     * - 요청 데이터: CreateOrderRequest DTO (안전한 타입)
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,    // ✅ 개선: SecurityContext의 Principal을 UserDetails로 받음
            @RequestBody CreateOrderRequest request              // ✅ 개선: Map 대신 DTO로 받아 타입 안전성 보장
    ) {
        // UserDetails에서 username(이메일) 추출하여 사용자 조회
        // (참고: 커스텀 UserDetails를 구현했다면 ((CustomUserDetails)userDetails).getUser()로 바로 엔티티를 꺼낼 수도 있음)
        User user = userService.findByEmail(userDetails.getUsername());

        // DTO 사용으로 불필요한 캐스팅(Integer cast) 제거 및 Null 안전성 확보
        Order order = orderService.createOrder(
                user,
                request.getProductId(),
                request.getQuantity()
        );

        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /**
     * 내 주문 목록 조회 API
     */
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails     // ✅ 개선: String 대신 UserDetails 사용
    ) {
        User user = userService.findByEmail(userDetails.getUsername()); // 이메일로 DB 조회

        List<OrderResponse> responses = orderService.getMyOrders(user).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 주문 취소(삭제) API
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,    // ✅ 개선: String 대신 UserDetails 사용
            @PathVariable Long orderId
    ) {
        User user = userService.findByEmail(userDetails.getUsername());

        // 주문 취소 로직 수행
        orderService.cancelOrder(orderId, user);

        return ResponseEntity.ok().build();
    }
}
