package com.commerceweb.dto; // 이 클래스가 속한 패키지(폴더/네임스페이스). DTO들을 모아 관리하기 위함.

import lombok.*; // Lombok 어노테이션들을 한 번에 import (getter/setter, 생성자, builder 등 자동 생성)
import java.time.LocalDateTime; // 리뷰 작성 시간을 표현하기 위한 날짜/시간 타입

/**
 * ReviewDto: "리뷰" 데이터를 계층 간(Controller ↔ Service ↔ Repository, 또는 API 요청/응답)으로 전달하기 위한 DTO.
 * - 엔티티(Entity)와 분리해 API 스펙/화면 요구사항에 맞게 필요한 필드만 담거나 가공해서 전달하는 용도.
 */
@Data // Lombok: getter/setter, toString, equals, hashCode 등을 자동 생성
@NoArgsConstructor // Lombok: 파라미터 없는 기본 생성자 생성 (JSON 역직렬화, 프레임워크 사용 시 유용)
@AllArgsConstructor // Lombok: 모든 필드를 받는 생성자 생성
@Builder // Lombok: 빌더 패턴 제공 (ReviewDto.builder().field(...).build())
public class ReviewDto {

    private Long id;
    // 리뷰 식별자(PK 역할). DB에 저장된 리뷰를 유일하게 구분할 때 사용.

    private Long productId;
    // 어떤 상품에 대한 리뷰인지 식별하는 상품 ID.

    private String productName;
    // 화면에서 "어떤 상품의 리뷰인지" 바로 보여주기 위해 추가한 상품명.

    private Long orderId;
    // 어떤 주문(구매)과 연관된 리뷰인지 나타내는 주문 ID.
    // "구매한 사람만 리뷰 작성" 같은 검증/추적에 활용 가능.

    private Long userId;
    // 리뷰 작성자 사용자 ID.

    private String userName;
    // 화면에 작성자 표시를 위해 사용자 이름(또는 닉네임)을 담는 필드.

    private String content;
    // 리뷰 본문(텍스트 내용).

    private Integer rating;
    // 평점(예: 1~5). Integer를 쓰면 null(미입력)도 표현 가능.

    private LocalDateTime createdAt;
    // 리뷰 작성/등록 시각.
}
