package com.commerceweb.integration;

import com.commerceweb.entity.Product;
import com.commerceweb.repository.OrderRepository;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.service.ProductService;
import com.commerceweb.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DisplayName("조회수 동시성 통합 테스트")
class ProductViewCountConcurrencyTest {

    @Autowired ProductService productService;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired NamedParameterJdbcTemplate jdbc;

    TestFixtures fx;

    @BeforeEach
    void setUp() {
        fx = new TestFixtures(userRepository, productRepository, orderRepository, jdbc);
    }

    @AfterEach
    void tearDown() {
        fx.cleanUp();
    }

    @Test
    @DisplayName("50명이 동시에 상세 조회하면 조회수가 정확히 50 늘어난다 (TS-11 Lost Update)")
    void concurrentViewsAreAllCounted() throws Exception {
        int n = 50;
        Product product = fx.product();

        List<Throwable> errors = TestFixtures.runConcurrently(n, i -> productService.getProduct(product.getId()));

        assertEquals(List.of(), errors);
        assertEquals(n, productRepository.findById(product.getId()).orElseThrow().getViewCount());
    }

    @Test
    @DisplayName("view_count가 NULL인 기존 상품도 1부터 센다")
    void nullViewCountStartsFromZero() {
        Product product = fx.product();
        jdbc.getJdbcTemplate().update("UPDATE products SET view_count = NULL WHERE id = ?", product.getId());

        Product viewed = productService.getProduct(product.getId());

        assertEquals(1L, viewed.getViewCount());
    }
}
