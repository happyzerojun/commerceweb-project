package com.commerceweb.service;

import com.commerceweb.entity.Product;
import com.commerceweb.entity.Rating;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

@DisplayName("추천 서비스 테스트")
class RecommendationServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("신규 사용자에게는 인기 상품을 추천한다")
    void testGetRecommendationsForNewUser() {
        // Given
        Long userId = 999L;
        int limit = 5;

        // 평가 기록 없음
        when(ratingRepository.findRatedProductIdsByUserId(userId))
                .thenReturn(List.of());

        // 인기 상품 반환
        Product popularProduct = Product.builder()
                .id(1L)
                .name("Popular Product")
                .averageRating(4.8)
                .build();

        when(productRepository.findHighRatedProducts())
                .thenReturn(List.of(popularProduct));

        // When
        List<Product> recommendations = recommendationService.getRecommendations(userId, limit);

        // Then
        assertEquals(1, recommendations.size());
        assertEquals("Popular Product", recommendations.get(0).getName());
        verify(ratingRepository, times(1)).findRatedProductIdsByUserId(userId);
        verify(productRepository, times(1)).findHighRatedProducts();
    }

    @Test
    @DisplayName("협력 필터링으로 중복 평가 상품을 제외한다")
    void testExcludeRatedProductsFromRecommendations() {
        // Given
        Long userId = 1L;
        int limit = 5;

        // 사용자가 이미 평가한 상품: [1, 2, 3]
        when(ratingRepository.findRatedProductIdsByUserId(userId))
                .thenReturn(List.of(1L, 2L, 3L));

        // 유사 사용자 찾기 (✅ 올바른 타입: List<Object[]>)
        List<Object[]> similarUsersList = new ArrayList<>();
        similarUsersList.add(new Object[]{2L, 2}); // 사용자2가 2개 상품 공통

        when(ratingRepository.findSimilarUsers(eq(userId), eq(List.of(1L, 2L, 3L))))
                .thenReturn(similarUsersList);

        // 사용자2의 평가: [상품1(5), 상품4(4), 상품5(5)]
        Product product1 = Product.builder().id(1L).build();
        Product product4 = Product.builder().id(4L).build();
        Product product5 = Product.builder().id(5L).build();

        Rating rating1 = Rating.builder().product(product1).score(5).build();
        Rating rating4 = Rating.builder().product(product4).score(4).build();
        Rating rating5 = Rating.builder().product(product5).score(5).build();

        when(ratingRepository.findByUserId(2L))
                .thenReturn(List.of(rating1, rating4, rating5));

        // 추천 상품 반환 (상품5가 더 높은 평점)
        when(productRepository.findByIdIn(List.of(5L, 4L)))
                .thenReturn(List.of(product5, product4));

        // When
        List<Product> recommendations = recommendationService.getRecommendations(userId, limit);

        // Then
        // 상품1은 이미 평가했으므로 추천에서 제외되어야 함
        assertEquals(2, recommendations.size());
        assertFalse(recommendations.stream()
                .anyMatch(p -> p.getId().equals(1L)));
        verify(ratingRepository, times(1)).findRatedProductIdsByUserId(userId);
        verify(ratingRepository, times(1)).findSimilarUsers(eq(userId), eq(List.of(1L, 2L, 3L)));
    }

    @Test
    @DisplayName("카테고리별 추천 상품을 조회할 수 있다")
    void testGetRecommendationsByCategory() {
        // Given
        String category = "electronics";
        int limit = 5;

        Product product1 = Product.builder()
                .id(1L)
                .name("Laptop")
                .category(category)
                .averageRating(4.7)
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .name("Mouse")
                .category(category)
                .averageRating(4.3)
                .build();

        when(productRepository.findByCategory(category))
                .thenReturn(List.of(product1, product2));

        // When
        List<Product> recommendations = recommendationService.getRecommendationsByCategory(category, limit);

        // Then
        assertEquals(2, recommendations.size());
        assertEquals(4.7, recommendations.get(0).getAverageRating());
        verify(productRepository, times(1)).findByCategory(category);
    }

    @Test
    @DisplayName("유사 사용자가 없으면 인기 상품을 추천한다")
    void testRecommendPopularProductsWhenNoSimilarUsers() {
        // Given
        Long userId = 1L;
        int limit = 5;

        // 평가 기록 있음
        when(ratingRepository.findRatedProductIdsByUserId(userId))
                .thenReturn(List.of(1L, 2L));

        // 유사 사용자 없음
        when(ratingRepository.findSimilarUsers(eq(userId), eq(List.of(1L, 2L))))
                .thenReturn(List.of());

        // 인기 상품으로 대체
        Product fallbackProduct = Product.builder()
                .id(5L)
                .name("Popular Fallback")
                .averageRating(4.9)
                .build();

        when(productRepository.findHighRatedProducts())
                .thenReturn(List.of(fallbackProduct));

        // When
        List<Product> recommendations = recommendationService.getRecommendations(userId, limit);

        // Then
        assertEquals(1, recommendations.size());
        assertEquals("Popular Fallback", recommendations.get(0).getName());
        verify(ratingRepository, times(1)).findSimilarUsers(eq(userId), eq(List.of(1L, 2L)));
    }
}
