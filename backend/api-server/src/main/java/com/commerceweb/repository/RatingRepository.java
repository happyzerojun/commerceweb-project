package com.commerceweb.repository;

import com.commerceweb.entity.Rating;
import com.commerceweb.entity.User;
import com.commerceweb.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    // ✅ 사용자의 모든 평가 조회
    List<Rating> findByUserId(Long userId);

    // ✅ 상품의 모든 평가 조회 (평점 재계산용)
    List<Rating> findByProductId(Long productId);

    // ✅ 사용자가 평가한 상품 ID 목록
    @Query("SELECT r.product.id FROM Rating r WHERE r.user.id = :userId")
    List<Long> findRatedProductIdsByUserId(@Param("userId") Long userId);

    // ✅ 유사 사용자 찾기 (협력 필터링)
    // 셀프 조인이 "내가 평가한 상품"을 이미 걸러 주므로 상품 ID 목록 파라미터는 필요 없다.
    // 이전 시그니처의 미사용 파라미터(ratedProductIds)는 -parameters 컴파일 시 이름 있는 파라미터로 인식돼
    // "parameter not found in annotated query" 검증에 걸릴 수 있었다. 이름은 @Param으로 명시한다 (TS-09)
    @Query(value = "SELECT r2.user_id, COUNT(*) as common_count " +
            "FROM ratings r1 " +
            "JOIN ratings r2 ON r1.product_id = r2.product_id " +
            "WHERE r1.user_id = :userId AND r2.user_id != :userId " +
            "GROUP BY r2.user_id " +
            "ORDER BY common_count DESC, r2.user_id " +
            "LIMIT 5",   // 서비스에서 상위 K=5명만 쓰므로 DB에서 잘라 전송량을 줄인다
            nativeQuery = true)
    List<Object[]> findSimilarUsers(@Param("userId") Long userId);

    // ✅ User + Product로 평가 조회
    Optional<Rating> findByUserAndProduct(User user, Product product);

    /**
     * (사용자, 상품)의 가장 최근 리뷰 별점을 ratings에 반영한다 (TS-08).
     * INSERT ... ON CONFLICT 한 문장이라 "조회 후 없으면 INSERT" 사이의 경쟁 조건이 없다.
     * uk(user_id, product_id) 유니크 제약이 ON CONFLICT 대상이다(Rating 엔티티에 선언).
     *
     * @return 반영된 행 수 (리뷰가 없으면 0)
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO ratings (user_id, product_id, score, review, created_at, updated_at) " +
            "SELECT latest.user_id, latest.product_id, latest.rating, latest.content, now(), now() FROM (" +
            "  SELECT r.user_id, r.product_id, r.rating, r.content FROM review r " +
            "  WHERE r.user_id = :userId AND r.product_id = :productId AND r.rating IS NOT NULL " +
            "  ORDER BY r.created_at DESC, r.id DESC LIMIT 1" +
            ") latest " +
            "ON CONFLICT (user_id, product_id) DO UPDATE " +
            "SET score = EXCLUDED.score, review = EXCLUDED.review, updated_at = now()",
            nativeQuery = true)
    int upsertFromLatestReview(@Param("userId") Long userId, @Param("productId") Long productId);

    /** (사용자, 상품)에 남은 리뷰가 없으면 추천용 평점도 지운다 (TS-08) */
    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM ratings WHERE user_id = :userId AND product_id = :productId " +
            "AND NOT EXISTS (SELECT 1 FROM review r WHERE r.user_id = :userId AND r.product_id = :productId AND r.rating IS NOT NULL)",
            nativeQuery = true)
    int deleteIfNoReview(@Param("userId") Long userId, @Param("productId") Long productId);
}
