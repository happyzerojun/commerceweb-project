package com.commerceweb.integration;

import com.commerceweb.dto.ReviewDto;
import com.commerceweb.entity.Order;
import com.commerceweb.entity.Product;
import com.commerceweb.entity.User;
import com.commerceweb.repository.OrderRepository;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.service.ReviewService;
import com.commerceweb.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 리뷰 쓰기 동시성 통합 테스트. 실제 PostgreSQL에 커밋하며, 끝나면 만든 데이터만 지운다.
 */
@SpringBootTest
@DisplayName("리뷰 동시성 통합 테스트")
class ReviewConcurrencyIntegrationTest {

    @Autowired ReviewService reviewService;
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
    @DisplayName("같은 주문에 리뷰를 동시에 10번 써도 1건만 저장되고 나머지는 409(IllegalStateException)다 (TS-06)")
    void duplicateReviewsOnSameOrderAreRejected() throws Exception {
        User buyer = fx.user();
        Product product = fx.product();
        Order order = fx.order(buyer, product);
        AtomicInteger success = new AtomicInteger();

        List<Throwable> errors = TestFixtures.runConcurrently(10, i -> {
            reviewService.createReview(review(order, buyer, 5));
            success.incrementAndGet();
        });

        assertEquals(1, success.get());
        assertEquals(9, errors.size());
        errors.forEach(e -> assertInstanceOf(IllegalStateException.class, e, () -> "예상하지 못한 예외: " + e));
        assertEquals(1, fx.count("SELECT COUNT(*) FROM review WHERE order_id = :o", "o", order.getId()));
    }

    static ReviewDto review(Order order, User buyer, int rating) {
        return ReviewDto.builder()
                .orderId(order.getId())
                .userId(buyer.getId())
                .content("concurrency test")
                .rating(rating)
                .build();
    }
}
