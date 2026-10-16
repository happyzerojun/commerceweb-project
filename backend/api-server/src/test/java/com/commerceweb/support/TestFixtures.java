package com.commerceweb.support;

import com.commerceweb.entity.Order;
import com.commerceweb.entity.Product;
import com.commerceweb.entity.User;
import com.commerceweb.entity.UserRole;
import com.commerceweb.repository.OrderRepository;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.UserRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * 커밋되는 통합 테스트(동시성 테스트)용 데이터 생성·정리 도우미.
 * 동시성 테스트는 트랜잭션 롤백 테스트로는 재현되지 않으므로(@Transactional 테스트는 스레드마다 트랜잭션이 따로 없다)
 * 실제로 커밋하고, 끝나면 만든 행만 지운다.
 */
public class TestFixtures {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final NamedParameterJdbcTemplate jdbc;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();

    public TestFixtures(UserRepository userRepository, ProductRepository productRepository,
                        OrderRepository orderRepository, NamedParameterJdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.jdbc = jdbc;
    }

    public User user() {
        User user = userRepository.save(User.builder()
                .email("it-" + UUID.randomUUID() + "@test.local")
                .password("x")
                .name("tester")
                .role(UserRole.CUSTOMER)
                .build());
        userIds.add(user.getId());
        return user;
    }

    public Product product() {
        Product product = productRepository.save(Product.builder()
                .name("it-product-" + UUID.randomUUID())
                .price(1000.0)
                .category("it-test")
                .viewCount(0L)
                .averageRating(0.0)
                .ratingCount(0L)
                .build());
        productIds.add(product.getId());
        return product;
    }

    public Order order(User user, Product product) {
        return orderRepository.save(Order.builder()
                .user(user)
                .product(product)
                .quantity(1)
                .totalPrice(1000L)
                .status("PAID")
                .shippingAddress("test")
                .build());
    }

    public long count(String sql, Object... keyValues) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (int i = 0; i < keyValues.length; i += 2) {
            params.addValue((String) keyValues[i], keyValues[i + 1]);
        }
        Long n = jdbc.queryForObject(sql, params, Long.class);
        return n == null ? 0 : n;
    }

    /** FK 순서대로 이 픽스처가 만든 데이터만 지운다 */
    public void cleanUp() {
        if (userIds.isEmpty() && productIds.isEmpty()) {
            return;
        }
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("u", userIds.isEmpty() ? List.of(-1L) : userIds)
                .addValue("p", productIds.isEmpty() ? List.of(-1L) : productIds);
        jdbc.update("DELETE FROM ratings WHERE user_id IN (:u) OR product_id IN (:p)", p);
        jdbc.update("DELETE FROM review WHERE user_id IN (:u) OR product_id IN (:p)", p);
        jdbc.update("DELETE FROM orders WHERE buyer_id IN (:u) OR product_id IN (:p)", p);
        jdbc.update("DELETE FROM products WHERE id IN (:p)", p);
        jdbc.update("DELETE FROM users WHERE id IN (:u)", p);
        userIds.clear();
        productIds.clear();
    }

    /**
     * 작업 n개를 시작 신호에 맞춰 동시에 실행하고, 각 작업에서 난 예외를 모아 돌려준다.
     */
    public static List<Throwable> runConcurrently(int n, IntConsumer task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        List<Throwable> errors = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            final int index = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.accept(index);
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        boolean finished = done.await(90, TimeUnit.SECONDS);
        pool.shutdownNow();
        if (!finished) {
            throw new IllegalStateException("동시 실행 작업이 90초 안에 끝나지 않았습니다 (교착 상태 의심)");
        }
        return errors;
    }
}
