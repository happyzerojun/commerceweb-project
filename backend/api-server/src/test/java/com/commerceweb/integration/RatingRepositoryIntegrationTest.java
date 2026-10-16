package com.commerceweb.integration;

import com.commerceweb.entity.Product;
import com.commerceweb.entity.Rating;
import com.commerceweb.entity.User;
import com.commerceweb.entity.UserRole;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.RatingRepository;
import com.commerceweb.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 유사 사용자 네이티브 쿼리를 실제 PostgreSQL에서 실행한다 (TS-09).
 * RecommendationService는 단계별 예외를 잡아 인기 상품으로 대체하므로, 쿼리가 깨져도 서비스 테스트는 통과할 수 있다.
 * 그래서 저장소 메서드를 직접 호출해 확인한다.
 */
@SpringBootTest
@Transactional
@DisplayName("RatingRepository 네이티브 쿼리 통합 테스트")
class RatingRepositoryIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired RatingRepository ratingRepository;

    @Test
    @DisplayName("같은 상품을 평가한 다른 사용자를 공통 평가 수와 함께 돌려준다")
    void findSimilarUsersRunsOnPostgres() {
        User me = user();
        User other = user();
        User stranger = user();
        Product shared = product();
        Product onlyOther = product();
        Product unrelated = product();

        rate(me, shared, 5);
        rate(other, shared, 4);
        rate(other, onlyOther, 3);
        rate(stranger, unrelated, 5);   // 공통 상품이 없으므로 결과에 없어야 함

        List<Object[]> similar = ratingRepository.findSimilarUsers(me.getId());

        assertEquals(1, similar.size());
        assertEquals(other.getId(), ((Number) similar.get(0)[0]).longValue());
        assertEquals(1L, ((Number) similar.get(0)[1]).longValue());
        assertEquals(List.of(shared.getId()), ratingRepository.findRatedProductIdsByUserId(me.getId()));
    }

    private User user() {
        return userRepository.save(User.builder()
                .email("repo-" + UUID.randomUUID() + "@test.local")
                .password("x").name("tester").role(UserRole.CUSTOMER).build());
    }

    private Product product() {
        return productRepository.save(Product.builder()
                .name("repo-product").price(1000.0).category("test")
                .viewCount(0L).averageRating(0.0).ratingCount(0L).build());
    }

    private void rate(User user, Product product, int score) {
        ratingRepository.saveAndFlush(Rating.builder().user(user).product(product).score(score).build());
    }
}
