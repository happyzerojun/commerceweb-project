package com.commerceweb.service;

import com.commerceweb.entity.Product;
import com.commerceweb.entity.Rating;
import com.commerceweb.entity.User;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.RatingRepository;
import com.commerceweb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

@DisplayName("상품 서비스 테스트")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("모든 상품을 조회할 수 있다")
    void testGetAllProducts() {
        // Given
        Product product1 = Product.builder()
                .id(1L)
                .name("Product 1")
                .price(10000.0)
                .averageRating(4.5)
                .build();
        Product product2 = Product.builder()
                .id(2L)
                .name("Product 2")
                .price(20000.0)
                .averageRating(4.0)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        // When
        List<Product> products = productService.getAllProducts();

        // Then
        assertEquals(2, products.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("상품 ID로 조회하면 조회수가 1 증가한다")
    void testGetProductIncreasesViewCount() {
        // Given
        Long productId = 1L;
        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .viewCount(5L)
                .averageRating(4.0)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // When
        Product result = productService.getProduct(productId);

        // Then
        assertEquals(6L, result.getViewCount());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("인기 상품을 조회할 수 있다")
    void testGetTopRatedProducts() {
        // Given
        Product topProduct = Product.builder()
                .id(1L)
                .name("Top Product")
                .averageRating(4.8)
                .build();

        when(productRepository.findAllByOrderByAverageRatingDesc())
                .thenReturn(List.of(topProduct));

        // When
        List<Product> products = productService.getTopRatedProducts();

        // Then
        assertEquals(1, products.size());
        assertEquals(4.8, products.get(0).getAverageRating());
        verify(productRepository, times(1)).findAllByOrderByAverageRatingDesc();
    }

    @Test
    @DisplayName("가격 범위로 상품을 검색할 수 있다")
    void testSearchByPriceRange() {
        // Given
        Double minPrice = 10000.0;
        Double maxPrice = 50000.0;
        Product product = Product.builder()
                .id(1L)
                .name("Mid-range Product")
                .price(30000.0)
                .build();

        when(productRepository.findByPriceBetween(minPrice, maxPrice))
                .thenReturn(List.of(product));

        // When
        List<Product> products = productService.searchByPriceRange(minPrice, maxPrice);

        // Then
        assertEquals(1, products.size());
        assertEquals(30000.0, products.get(0).getPrice());
        verify(productRepository, times(1)).findByPriceBetween(minPrice, maxPrice);
    }
}
