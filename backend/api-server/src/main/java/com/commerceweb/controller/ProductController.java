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

@RestController
@RequestMapping({"/api/products", "/products"})
@RequiredArgsConstructor
@Slf4j
// ✅ 이 한 줄이 없으면 프론트엔드(5173 포트)에서 데이터를 못 가져옵니다.
// ✅ 개발 서버(5173)와 도커 배포 서버(80) 둘 다 허용
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost"})
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 (검색 기능 포함)
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String name) {

        log.debug("상품 목록 조회 요청 - category: {}, name: {}", category, name);
        List<Product> products = productService.getProducts(category, name);

        // Entity → DTO 변환
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        Product product = productService.getProduct(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    /**
     * 인기 상품 조회
     */
    @GetMapping("/trending/popular")
    public ResponseEntity<List<ProductResponse>> getPopularProducts() {
        List<Product> products = productService.getTopRatedProducts();
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * 트렌딩 상품 조회
     */
    @GetMapping("/trending/trending")
    public ResponseEntity<List<ProductResponse>> getTrendingProducts() {
        List<Product> products = productService.getTrendingProducts();
        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}