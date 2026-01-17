package com.commerceweb.controller;

import java.util.List;

import com.commerceweb.dto.product.ProductResponse;
import com.commerceweb.dto.product.RecommendationResponse;
import com.commerceweb.entity.User;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.service.RecommendationService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 추천 관련 API를 처리하는 컨트롤러
 * 사용자의 이전 활동이나 데이터를 기반으로 맞춤 상품을 추천합니다.
 */
@RestController // @Controller + @ResponseBody, JSON 형태의 데이터를 반환함을 명시
@RequestMapping("/api") // 이 컨트롤러의 기본 URL 경로 설정
@RequiredArgsConstructor // final로 선언된 필드의 생성자를 자동으로 생성 (의존성 주입)
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    /**
     * 사용자 맞춤 추천 상품 조회 엔드포인트
     * GET /api/recommendations?topN=5
     *
     * @param topN 반환할 추천 상품의 개수 (파라미터가 없으면 기본값 5 사용)
     * @return 추천 상품 리스트가 담긴 DTO
     */
    @GetMapping("/recommendations")
    public RecommendationResponse getRecommendations(@RequestParam(defaultValue = "5") int topN) {
        // 1. 현재 요청을 보낸 사용자의 ID(PK)를 보안 컨텍스트에서 추출
        Long userId = getCurrentUserId();

        // 2. 서비스 계층을 호출하여 추천 상품 목록을 가져온 후, 응답용 DTO로 변환
        // - recommendationService.getRecommendations(userId, topN): 비즈니스 로직 수행
        // - stream().map(ProductResponse::from): 엔티티(또는 내부 객체)를 클라이언트에 전달할 ProductResponse로 매핑
        List<ProductResponse> products = recommendationService.getRecommendations(userId, topN).stream()
                .map(ProductResponse::from)
                .toList(); // Java 16+의 Stream.toList() 사용 (불변 리스트 반환)

        // 3. 최종 응답 객체(Wrapper DTO)로 감싸서 반환
        return new RecommendationResponse(products);
    }

    /**
     * Spring Security Context에서 현재 인증된 사용자의 정보를 가져오는 헬퍼 메서드
     * JWT 필터 등을 거쳐 SecurityContext에 저장된 인증 정보(Authentication)를 활용합니다.
     *
     * @return 조회된 사용자의 DB ID (Long)
     * @throws RuntimeException 해당 이메일을 가진 사용자가 DB에 없을 경우 예외 발생
     */
    private Long getCurrentUserId() {
        // 현재 스레드의 SecurityContext에서 인증 객체를 가져옴
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 인증 객체의 Principal(주체)에서 이름을 가져옴
        // 보통 JWT 설정 시 Subject에 이메일이나 사용자 ID를 넣으므로, 여기서는 이메일로 가정
        String email = auth.getName();

        // DB에서 이메일로 사용자 정보를 조회하여 ID 반환
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        return user.getId();
    }
}
