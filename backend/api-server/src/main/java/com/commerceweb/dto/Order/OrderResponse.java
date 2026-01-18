package com.commerceweb.dto.Order;

import com.commerceweb.entity.Order;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ========================================
 * OrderResponse DTO (Data Transfer Object)
 * ========================================
 *
 * 【DTO의 정의】
 * DTO는 계층 간(Layer) 데이터 전송을 위한 객체로, 엔티티와 분리된 독립적인 데이터 구조를 제공합니다.
 * - 계층: Controller ↔ Service ↔ Repository ↔ Database
 * - 역할: 클라이언트에게 필요한 데이터만 선택적으로 노출
 *
 * 【DTO를 사용하는 이유】
 *
 * 1. 관심사의 분리 (Separation of Concerns)
 *    - DB 엔티티(Order)와 API 응답(OrderResponse)을 분리
 *    - 엔티티 구조 변경이 API 응답에 영향을 주지 않음
 *
 * 2. 보안성 (Security)
 *    - 클라이언트에게 불필요한 필드(예: 내부 ID, 시스템 정보) 노출 방지
 *    - 예: 결제 시스템 내부 토큰, 관리자 플래그 등을 숨길 수 있음
 *
 * 3. 성능 최적화 (Performance)
 *    - 필요한 필드만 선택적으로 포함 (N+1 쿼리 문제 해결)
 *    - 대량의 불필요한 데이터 전송 방지
 *    - 네트워크 대역폭 절감
 *
 * 4. API 계약 유지 (API Contract)
 *    - 클라이언트와의 계약(응답 형식)을 일정하게 유지
 *    - 엔티티 리팩토링 시 API는 변경 없음
 *
 * 5. 유효성 검증 (Validation)
 *    - DTO에서 요청/응답 데이터의 유효성 검증
 *    - @NotNull, @Email 등의 애너테이션 활용
 *
 * 【예시】
 * Order 엔티티가 다음 필드를 가짐:
 * - id, productId, productName, quantity, totalPrice, status, orderDate
 * - internalSystemNotes (내부 시스템 노트)
 * - paymentGatewayToken (결제 게이트웨이 토큰)
 * - isReturned (반품 여부, 내부용)
 *
 * OrderResponse DTO는 필요한 필드만 선택:
 * - id, productId, productName, quantity, totalPrice, status, orderDate
 * - 민감한 정보(토큰, 내부 노트)는 제외
 *
 * ========================================
 */
@Getter
@Builder
public class OrderResponse {

    /**
     * 주문 고유 ID
     * - 클라이언트가 주문을 식별하는 데 사용
     * - 데이터베이스의 Primary Key
     */
    private Long id;

    /**
     * 상품 ID
     * - ✅ 클라이언트 측에서 어떤 상품인지 식별하기 위해 필요
     * - 상품 상세 정보 조회 시 사용
     * - 추천 알고리즘, 재구매 기능에 활용 가능
     *
     * 【추가된 이유】
     * 클라이언트가 주문 응답을 받을 때:
     * - productId 없이: productName만 있어서 상품 상세를 조회할 때 추가 쿼리 필요
     * - productId 포함: 상품 상세 API를 직접 호출 가능
     */
    private Long productId;

    /**
     * 상품명
     * - 클라이언트에게 사용자 친화적 정보 제공
     * - 주문 목록에서 직접 표시
     */
    private String productName;

    /**
     * 주문 수량
     * - 고객이 주문한 상품의 개수
     * - 리뷰, 재구매 시 참고 정보
     */
    private Integer quantity;

    /**
     * 주문 총액
     * - 가격 = 상품 단가 × 수량
     * - 결제 영수증, 통계 정보에 사용
     */
    private Long totalPrice;

    /**
     * 주문 상태
     * - 가능한 값: "PENDING" (대기), "CONFIRMED" (확인), "SHIPPED" (배송중), "DELIVERED" (배송완료), "CANCELLED" (취소)
     * - 클라이언트 UI에서 주문 상태 표시에 사용
     * - 상태에 따라 다른 액션(취소, 반품) 제공
     */
    private String status;

    /**
     * 주문 생성 일시
     * - 주문한 정확한 시간
     * - 시간순 정렬, 주문 이력 추적에 사용
     */
    private LocalDateTime orderDate;

    /**
     * ========================================
     * from() 메서드 (Entity → DTO 변환)
     * ========================================
     *
     * 【목적】
     * - JPA 엔티티 객체를 API 응답용 DTO로 변환
     * - 엔티티의 모든 필드가 아닌, 필요한 필드만 응답으로 전송
     * - Lazy Loading 이슈 방지 (명시적으로 필요한 데이터만 로드)
     *
     * 【흐름】
     * Repository.findById() → Order 엔티티
     *         ↓
     * Service에서 from() 호출
     *         ↓
     * OrderResponse DTO 생성 (필요한 필드만)
     *         ↓
     * Controller에서 JSON으로 직렬화
     *         ↓
     * 클라이언트가 JSON 응답 수신
     *
     * 【예시 동작】
     * Order order = orderRepository.findById(1L); // DB에서 조회
     * OrderResponse response = OrderResponse.from(order); // DTO 변환
     * return ResponseEntity.ok(response); // JSON으로 반환
     *
     * 【결과】
     * {
     *   "id": 1,
     *   "productId": 100,
     *   "productName": "MacBook Pro 14인치",
     *   "quantity": 1,
     *   "totalPrice": 2990000,
     *   "status": "DELIVERED",
     *   "orderDate": "2026-01-18T10:30:00"
     * }
     */
    public static OrderResponse from(Order order) {
        // ✅ Builder 패턴으로 DTO 구성
        // - null 체크: order.getProduct() 호출 시 NPE 방지 필요
        // - 대안: order.getProduct()?.getId() (Optional 처리)
        return OrderResponse.builder()
                // 주문 ID는 엔티티에서 직접 가져옴
                .id(order.getId())

                // ✅ 상품 ID 명시적 추출
                // 이유: 클라이언트가 상품을 식별해야 하므로
                // 주의: order.getProduct()가 null일 수 있으므로 방어 로직 권장
                .productId(order.getProduct().getId())

                // 상품명은 Product 엔티티에서 추출
                // 주의: 상품이 삭제되면 null이 될 수 있음 → 처리 필요
                .productName(order.getProduct().getName())

                // 주문 수량
                .quantity(order.getQuantity())

                // 총 주문 금액
                .totalPrice(order.getTotalPrice())

                // 주문 상태
                .status(order.getStatus())

                // 주문 생성 일시
                .orderDate(order.getOrderDate())

                // Builder 완성
                .build();
    }
}

/**
 * ========================================
 * 개선 제안 (Advanced Best Practices)
 * ========================================
 *
 * 현재 코드의 잠재적 문제와 해결책:
 *
 * 1. 【Null Pointer Exception】
 *    문제: order.getProduct()가 null이면 NPE 발생
 *    해결책: Optional 또는 null 체크
 *
 *    개선된 코드:
 *    public static OrderResponse from(Order order) {
 *        if (order.getProduct() == null) {
 *            throw new IllegalArgumentException("상품 정보가 없는 주문입니다");
 *        }
 *        // ... 기존 코드
 *    }
 *
 * 2. 【Lazy Loading 문제】
 *    문제: @Transactional 밖에서 order.getProduct() 호출 시 LazyInitializationException
 *    해결책: @Transactional(readOnly=true) 또는 Fetch Join
 *
 *    쿼리 개선:
 *    @Query("SELECT o FROM Order o JOIN FETCH o.product WHERE o.id = :id")
 *    Optional<Order> findByIdWithProduct(@Param("id") Long id);
 *
 * 3. 【버전 관리】
 *    문제: API 응답 형식 변경 시 기존 클라이언트 호환성 문제
 *    해결책: @ApiVersion, @Deprecated, 신규 엔드포인트 생성
 *
 *    예시:
 *    @GetMapping("/v1/orders/{id}") // 기존
 *    public ResponseEntity<OrderResponse> getOrderV1(...) { ... }
 *
 *    @GetMapping("/v2/orders/{id}") // 신규 (확장 필드)
 *    public ResponseEntity<OrderResponseV2> getOrderV2(...) { ... }
 *
 * 4. 【매핑 라이브러리 활용】
 *    현재: 수동 매핑 (유지보수 부담)
 *    개선: MapStruct, ModelMapper로 자동 매핑
 *
 *    예시:
 *    @Mapper(componentModel = "spring")
 *    public interface OrderMapper {
 *        OrderResponse toResponse(Order order);
 *    }
 *
 * ========================================
 */