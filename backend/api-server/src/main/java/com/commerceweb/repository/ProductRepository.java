package com.commerceweb.repository;

import com.commerceweb.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ✅ 카테고리와 이름으로 검색 (대소문자 무시)
    List<Product> findByCategoryAndNameContainingIgnoreCase(String category, String name);

    // ✅ [기존 유지] 평점 높은 순 정렬 (전체)
    List<Product> findAllByOrderByAverageRatingDesc();

    // ✅ [기존 유지] 조회수 높은 순 정렬 (전체)
    List<Product> findAllByOrderByViewCountDesc();

    // ✅ (해결 - reviewCount 대신 viewCount 사용)
    // "평점이 같으면 조회수(viewCount)가 높은 순으로 정렬해라"
    List<Product> findTop10ByOrderByAverageRatingDescViewCountDesc();

    // ✅ [NEW] 트렌딩 상품용: 가장 최근에 등록된 상품 (상위 10개)
    // (만약 CreatedAt 필드가 없다면 ID 역순인 findTop10ByOrderByIdDesc() 사용 가능)
    List<Product> findTop10ByOrderByCreatedAtDesc();

    // ✅ 가격 범위 검색
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    // ✅ 카테고리별 검색
    List<Product> findByCategory(String category);

    // ✅ 이름으로 검색 (대소문자 무시)
    List<Product> findByNameContainingIgnoreCase(String name);

    // ✅ 평점 4.0 이상인 상품 조회 (인기 상품 추천용 - 커스텀 쿼리)
    @Query("SELECT p FROM Product p WHERE p.averageRating >= 4.0 ORDER BY p.averageRating DESC")
    List<Product> findHighRatedProducts();

    // ✅ ID 목록으로 상품 조회 (추천 시스템용)
    List<Product> findByIdIn(List<Long> ids);

    /**
     * 조회수 원자적 증가 (TS-11). 읽고 +1 해서 save()하면 동시 조회 시 같은 값을 읽어 증가분이 사라졌다.
     * DB가 행 잠금 아래에서 현재 값에 더하므로 동시 요청 수만큼 정확히 늘어난다.
     * COALESCE: @Builder로 만든 상품은 필드 초기값이 무시돼 view_count가 NULL일 수 있다(NULL + 1 = NULL).
     * clearAutomatically: 이후 findById가 1차 캐시의 옛 값이 아니라 갱신된 값을 읽도록.
     *
     * @return 갱신된 행 수 (0이면 상품 없음)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.viewCount = COALESCE(p.viewCount, 0L) + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 평점 재계산 전에 상품 행을 잠가 같은 상품의 리뷰 쓰기를 직렬화한다 (TS-10).
     * <p>
     * PostgreSQL READ COMMITTED에서 UPDATE ... SET x = (SELECT AVG ...)는 단일 문장이어도 안전하지 않다.
     * 행 잠금을 기다린 뒤 대상 행만 최신 버전으로 다시 읽고, 서브쿼리는 문장 시작 시점 스냅샷을 그대로 쓰기 때문에
     * 먼저 커밋된 다른 리뷰가 평균에서 빠질 수 있다. 잠금을 먼저 잡으면 다음 문장은 새 스냅샷으로 시작한다.
     * <p>
     * FOR UPDATE가 아니라 FOR NO KEY UPDATE인 이유: 리뷰·주문 INSERT의 FK 검사가 상품 행에 거는 FOR KEY SHARE와
     * 충돌하지 않아, "서로 KEY SHARE를 잡은 채 FOR UPDATE를 기다리는" 교착 상태가 생기지 않는다.
     */
    @Query(value = "SELECT id FROM products WHERE id = :id FOR NO KEY UPDATE", nativeQuery = true)
    Long lockForRatingUpdate(@Param("id") Long id);

    /**
     * 상품 평균 평점·평점 수를 리뷰 테이블에서 DB가 직접 집계해 한 문장으로 덮어쓴다 (TS-10).
     * 애플리케이션에서 읽고 계산해 save()하면 동시 작성 시 나중 커밋이 앞선 결과를 덮었다(Lost Update).
     * flushAutomatically: 같은 트랜잭션에서 아직 flush되지 않은 리뷰 변경을 집계에 포함시키기 위해.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE products SET " +
            "average_rating = COALESCE((SELECT ROUND(AVG(r.rating), 1) FROM review r WHERE r.product_id = :id), 0), " +
            "rating_count = (SELECT COUNT(r.rating) FROM review r WHERE r.product_id = :id), " +
            "updated_at = now() " +
            "WHERE id = :id", nativeQuery = true)
    int refreshRatingStats(@Param("id") Long id);
}
