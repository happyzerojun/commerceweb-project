package com.commerceweb.integration;

import com.commerceweb.dto.ReviewDto;
import com.commerceweb.entity.Order;
import com.commerceweb.entity.Product;
import com.commerceweb.entity.User;
import com.commerceweb.repository.OrderRepository;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.service.RecommendationService;
import com.commerceweb.service.ReviewService;
import com.commerceweb.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 리뷰 → ratings 동기화, 추천 캐시 무효화·TTL 통합 테스트 (TS-08). 실제 PostgreSQL·Redis 사용.
 */
@SpringBootTest
@DisplayName("추천 데이터·캐시 통합 테스트")
class RecommendationCacheIntegrationTest {

    @Autowired ReviewService reviewService;
    @Autowired RecommendationService recommendationService;
    @Autowired CacheManager cacheManager;
    @Autowired StringRedisTemplate redis;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired NamedParameterJdbcTemplate jdbc;

    TestFixtures fx;
    Cache cache;

    @BeforeEach
    void setUp() {
        fx = new TestFixtures(userRepository, productRepository, orderRepository, jdbc);
        cache = cacheManager.getCache("recommendations");
    }

    @AfterEach
    void tearDown() {
        fx.cleanUp();
        cache.clear();
    }

    @Test
    @DisplayName("추천 캐시 항목에는 1시간 TTL이 걸린다")
    void recommendationCacheHasTtl() {
        cache.put("ttl-check", new ArrayList<>(List.of("x")));

        Long ttl = redis.getExpire("recommendations::ttl-check", TimeUnit.SECONDS);

        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 3600, "TTL(초)이 0 < ttl <= 3600 이어야 함: " + ttl);
    }

    @Test
    @DisplayName("리뷰 작성·삭제가 커밋되면 추천 캐시가 비워진다")
    void reviewWriteEvictsRecommendationCache() {
        User buyer = fx.user();
        Product product = fx.product();

        cache.put("sentinel", new ArrayList<>());
        ReviewDto created = reviewService.createReview(review(fx.order(buyer, product), buyer, 4));
        assertNull(cache.get("sentinel"), "리뷰 작성 후 캐시가 남아 있음");

        cache.put("sentinel", new ArrayList<>());
        reviewService.deleteReview(created.getId(), buyer);
        assertNull(cache.get("sentinel"), "리뷰 삭제 후 캐시가 남아 있음");
    }

    @Test
    @DisplayName("실패한(롤백된) 리뷰 쓰기는 캐시를 지우지 않는다")
    void failedReviewWriteKeepsCache() {
        User buyer = fx.user();
        User other = fx.user();
        Order order = fx.order(buyer, fx.product());

        cache.put("sentinel", new ArrayList<>());
        assertThrows(RuntimeException.class, () -> reviewService.createReview(review(order, other, 5)));

        assertNotNull(cache.get("sentinel"));
    }

    @Test
    @DisplayName("리뷰만으로 협업 필터링 추천이 동작한다: 리뷰 → ratings → 유사 사용자 → 추천")
    void reviewsDriveCollaborativeFiltering() {
        User me = fx.user();
        User similar = fx.user();
        Product shared = fx.product();
        Product candidate = fx.product();

        reviewService.createReview(review(fx.order(me, shared), me, 5));
        reviewService.createReview(review(fx.order(similar, shared), similar, 5));
        // 평균 3.0이라 인기 상품(평균 4.0 이상) 대체 경로로는 나올 수 없다 → 나오면 협업 필터링 결과
        reviewService.createReview(review(fx.order(similar, candidate), similar, 3));

        assertEquals(1, fx.count("SELECT COUNT(*) FROM ratings WHERE user_id = :u", "u", me.getId()));
        assertEquals(2, fx.count("SELECT COUNT(*) FROM ratings WHERE user_id = :u", "u", similar.getId()));

        List<Product> recommended = recommendationService.getRecommendations(me.getId(), 5);

        assertEquals(List.of(candidate.getId()), recommended.stream().map(Product::getId).toList());
    }

    @Test
    @DisplayName("같은 상품 리뷰가 여러 개면 최신 별점 1행, 모두 지우면 ratings도 삭제")
    void ratingsFollowLatestReviewAndDeletion() {
        User buyer = fx.user();
        Product product = fx.product();

        ReviewDto first = reviewService.createReview(review(fx.order(buyer, product), buyer, 2));
        ReviewDto second = reviewService.createReview(review(fx.order(buyer, product), buyer, 5));
        assertEquals(1, ratingRows(buyer, product));
        assertEquals(5, score(buyer, product));

        reviewService.updateReview(second.getId(), "changed", 1, buyer.getId());
        assertEquals(1, score(buyer, product));

        reviewService.deleteReview(second.getId(), buyer);
        assertEquals(2, score(buyer, product));   // 남은 리뷰의 별점으로 돌아감

        reviewService.deleteReview(first.getId(), buyer);
        assertEquals(0, ratingRows(buyer, product));
    }

    private long ratingRows(User u, Product p) {
        return fx.count("SELECT COUNT(*) FROM ratings WHERE user_id = :u AND product_id = :p", "u", u.getId(), "p", p.getId());
    }

    private long score(User u, Product p) {
        return fx.count("SELECT score FROM ratings WHERE user_id = :u AND product_id = :p", "u", u.getId(), "p", p.getId());
    }

    private static ReviewDto review(Order order, User buyer, int rating) {
        return ReviewDto.builder().orderId(order.getId()).userId(buyer.getId()).content("cf test").rating(rating).build();
    }
}
