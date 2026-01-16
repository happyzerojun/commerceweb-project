package com.commerceweb.controller;

import com.commerceweb.dto.product.ProductResponse;
import com.commerceweb.entity.Product;
import com.commerceweb.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 관련 HTTP 요청을 처리하는 컨트롤러 클래스
 * <p>
 * 프론트엔드로부터 들어오는 상품 조회, 검색, 필터링 요청을 받아
 * Service 계층으로 전달하고, 결과를 DTO로 변환하여 반환합니다.
 * </p>
 */
@RestController // @Controller + @ResponseBody 기능 포함 (JSON 형태로 객체 데이터 반환)
// 클라이언트가 호출할 기본 URL 경로 설정 (하위 호환성을 위해 두 가지 경로 모두 매핑)
@RequestMapping({"/api/products", "/products"})
@RequiredArgsConstructor // final이 붙은 필드에 대한 생성자를 자동으로 생성 (의존성 주입)
@Slf4j // 로깅 기능(Logger) 자동 생성
// ✅ CORS(Cross-Origin Resource Sharing) 설정
// - 개발 환경(localhost:5173 - React/Vite)과 배포 환경(localhost:80 - Nginx/Docker) 모두 허용
// - 이 설정이 없으면 브라우저 보안 정책에 의해 프론트엔드에서 API 호출이 차단됨
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost"})
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 및 검색 API
     * <p>
     * GET /api/products?category=electronics&name=iphone
     * </p>
     *
     * @param category 카테고리 필터 (선택 사항, null일 경우 전체 조회)
     * @param name 상품명 검색어 (선택 사항, null일 경우 전체 조회)
     * @return 검색 조건에 맞는 ProductResponse 리스트와 HTTP 200 상태 코드
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String category, // 쿼리 파라미터 (?key=value) 수신
            @RequestParam(required = false) String name) {

        // 디버깅을 위한 요청 파라미터 로그 출력
        log.debug("상품 목록 조회 요청 - category: {}, name: {}", category, name);

        // Service 계층을 호출하여 비즈니스 로직 수행 (Entity 리스트 반환)
        List<Product> products = productService.getProducts(category, name);

        // [Entity → DTO 변환]
        // 1. Entity는 DB 테이블과 직접 매핑되므로 API 응답으로 직접 노출하는 것은 보안/유지보수 상 좋지 않음
        // 2. Stream API를 사용하여 Product 리스트를 ProductResponse 리스트로 매핑
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from) // ProductResponse 클래스의 static 팩토리 메서드 활용
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 상품 상세 조회 API
     * <p>
     * GET /api/products/{id}
     * </p>
     *
     * @param id 조회할 상품의 고유 식별자 (DB PK)
     * @return 단일 상품 상세 정보 (ProductResponse)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) { // URL 경로에 있는 변수({id})를 매핑
        // Service에서 ID로 상품 조회 (존재하지 않을 경우 Service 내부에서 예외 처리 예상)
        Product product = productService.getProduct(id);

        // Entity를 DTO로 변환하여 반환
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    /**
     * 인기 상품 조회 API (별점/평점 기준 상위 상품)
     * <p>
     * GET /api/products/trending/popular
     * 주로 메인 페이지의 '인기 상품' 섹션에서 사용됩니다.
     * </p>
     *
     * @return 평점이 높은 순으로 정렬된 상품 리스트
     */
    @GetMapping("/trending/popular")
    public ResponseEntity<List<ProductResponse>> getPopularProducts() {
        // Service에서 평점 기준 상위 상품 리스트 조회
        List<Product> products = productService.getTopRatedProducts();

        // Stream을 이용해 DTO 리스트로 변환
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 트렌딩 상품 조회 API (최신/판매량 급상승 등 기준)
     * <p>
     * GET /api/products/trending/trending
     * 주로 메인 페이지의 '요즘 뜨는 상품' 섹션에서 사용됩니다.
     * </p>
     *
     * @return 트렌드 로직에 따라 선정된 상품 리스트
     */
    @GetMapping("/trending/trending")
    public ResponseEntity<List<ProductResponse>> getTrendingProducts() {
        // Service에서 트렌딩 로직에 맞는 상품 리스트 조회
        List<Product> products = productService.getTrendingProducts();

        // Stream을 이용해 DTO 리스트로 변환
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
