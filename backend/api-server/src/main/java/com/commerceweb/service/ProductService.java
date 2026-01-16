package com.commerceweb.service;

import com.commerceweb.entity.Product;
import com.commerceweb.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 모든 상품 조회
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.info("📦 모든 상품 조회");
        return productRepository.findAll();
    }

    /**
     * 상품 검색 (카테고리 + 이름)
     */
    @Transactional(readOnly = true)
    public List<Product> getProducts(String category, String name) {
        log.info("🔍 상품 검색: category={}, name={}", category, name);

        if (category != null && !category.isEmpty()) {
            if (name != null && !name.isEmpty()) {
                return productRepository.findByCategoryAndNameContainingIgnoreCase(category, name);
            } else {
                return productRepository.findByCategory(category);
            }
        } else if (name != null && !name.isEmpty()) {
            return productRepository.findByNameContainingIgnoreCase(name);
        } else {
            return productRepository.findAll();
        }
    }

    /**
     * 단일 상품 상세 조회 (조회수 증가 포함)
     */
    @Transactional
    public Product getProduct(Long id) {
        log.info("🔎 상품 상세 조회: id={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + id));

        // 조회수 1 증가
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        return product;
    }

    /**
     * 상품 등록
     */
    @Transactional
    public Product createProduct(Product product) {
        log.info("✨ 새 상품 등록: {}", product.getName());
        return productRepository.save(product);
    }

    /**
     * 상품 수정
     */
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        log.info("📝 상품 수정: id={}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + id));

        if (updatedProduct.getName() != null) product.setName(updatedProduct.getName());
        if (updatedProduct.getPrice() != null) product.setPrice(updatedProduct.getPrice());
        if (updatedProduct.getDescription() != null) product.setDescription(updatedProduct.getDescription());
        if (updatedProduct.getCategory() != null) product.setCategory(updatedProduct.getCategory());
        if (updatedProduct.getImageUrl() != null) product.setImageUrl(updatedProduct.getImageUrl());

        return productRepository.save(product);
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("🗑️ 상품 삭제: id={}", id);
        productRepository.deleteById(id);
    }

    /**
     * 🔥 인기 상품 (평점 높고 + 리뷰 많은 순, 상위 10개)
     * - 기존: 전체 평점순 조회
     * - 변경: 상위 10개만 조회 + 리뷰 수 가중치
     */
    @Transactional(readOnly = true)
    public List<Product> getTopRatedProducts() {
        log.info("🏆 인기 상품 조회 (Top 10)");
        // return productRepository.findAllByOrderByAverageRatingDesc(); // (구버전)
        // return productRepository.findTop10ByOrderByAverageRatingDescReviewCountDesc(); (X)
        return productRepository.findTop10ByOrderByAverageRatingDescViewCountDesc(); // (O)
    }

    /**
     * ⚡ 트렌딩 상품 (최신 등록된 신상품, 상위 10개)
     * - 기존: 조회수 전체 조회 (고인물 문제 발생 가능)
     * - 변경: 가장 최근에 등록된 신상 10개 노출
     */
    @Transactional(readOnly = true)
    public List<Product> getTrendingProducts() {
        log.info("📈 트렌딩 상품 조회 (New Arrivals Top 10)");
        // return productRepository.findAllByOrderByViewCountDesc(); // (구버전)

        // 만약 Product 엔티티에 createdAt 필드가 있다면 아래 사용:
        return productRepository.findTop10ByOrderByCreatedAtDesc();

        // ⚠️ 만약 createdAt 필드가 없다면, ID 역순이 곧 등록순이므로 아래 사용:
        // return productRepository.findTop10ByOrderByIdDesc();
    }

    /**
     * 가격 범위 검색
     */
    @Transactional(readOnly = true)
    public List<Product> searchByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    /**
     * 카테고리별 조회
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
}
