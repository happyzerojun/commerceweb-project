package com.commerceweb.integration;

import com.commerceweb.entity.Product;
import com.commerceweb.entity.Rating;
import com.commerceweb.entity.User;
import com.commerceweb.entity.UserRole;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.RatingRepository;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 추천 시스템 통합 테스트
 */
@SpringBootTest
@DisplayName("추천 시스템 통합 테스트")
@Transactional
class RecommendationIntegrationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RatingRepository ratingRepository;

    private User testUser;
    private Product product1, product2, product3;

    @BeforeEach
    void setUp() {
        // ✅ 1. 테스트 사용자 생성 (Builder 패턴)
        testUser = User.builder()
                .email("testuser@example.com")
                .password("encoded_password")
                .name("Test User")
                .role(UserRole.CUSTOMER)
                .build();
        testUser = userRepository.save(testUser);

        // ✅ 2. 테스트 상품 생성
        product1 = Product.builder()
                .name("Electronics Product 1")
                .price(100.0)
                .category("electronics")
                .build();
        product1 = productRepository.save(product1);

        product2 = Product.builder()
                .name("Electronics Product 2")
                .price(200.0)
                .category("electronics")
                .build();
        product2 = productRepository.save(product2);

        product3 = Product.builder()
                .name("Books Product 1")
                .price(300.0)
                .category("books")
                .build();
        product3 = productRepository.save(product3);

        // ✅ 3. 평점 생성 (Builder 패턴 + 올바른 타입)
        Rating rating1 = Rating.builder()
                .user(testUser)              // ✅ User 객체 설정
                .score(5)                    // ✅ Integer 타입
                .product(product1)
                .review("훌륭한 상품입니다!")
                .build();
        ratingRepository.save(rating1);

        Rating rating2 = Rating.builder()
                .user(testUser)              // ✅ User 객체 설정
                .score(4)                    // ✅ Integer 타입
                .product(product2)
                .review("좋은 상품입니다!")
                .build();
        ratingRepository.save(rating2);

        Rating rating3 = Rating.builder()
                .user(testUser)              // ✅ User 객체 설정
                .score(3)                    // ✅ Integer 타입
                .product(product3)
                .review("보통 상품입니다!")
                .build();
        ratingRepository.save(rating3);
    }

    @Test
    @DisplayName("사용자 추천 상품 조회")
    void testGetRecommendations() {
        // When
        List<Product> recommendations = recommendationService
                .getRecommendations(testUser.getId(), 10);

        // Then
        assertNotNull(recommendations);
        System.out.println("✅ 추천 상품 개수: " + recommendations.size());
    }

    @Test
    @DisplayName("카테고리별 상품 추천")
    void testGetRecommendationsByCategory() {
        // When
        List<Product> recommendations = recommendationService
                .getRecommendationsByCategory("electronics", 5);

        // Then
        assertNotNull(recommendations);
        System.out.println("✅ 카테고리 추천: " + recommendations.size() + "개");
    }

    @Test
    @DisplayName("인기 상품 조회")
    void testGetPopularProducts() {
        // When
        List<Product> popular = recommendationService.getPopularProducts(10);

        // Then
        assertNotNull(popular);
        System.out.println("✅ 인기 상품: " + popular.size() + "개");
    }
}
