package com.commerceweb.repository;

import com.commerceweb.entity.Rating;
import com.commerceweb.entity.User;
import com.commerceweb.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    List<Long> findRatedProductIdsByUserId(Long userId);

    // ✅ 유사 사용자 찾기 (협력 필터링)
    @Query(value = "SELECT r2.user_id, COUNT(*) as common_count " +
            "FROM ratings r1 " +
            "JOIN ratings r2 ON r1.product_id = r2.product_id " +
            "WHERE r1.user_id = :userId AND r2.user_id != :userId " +
            "GROUP BY r2.user_id " +
            "ORDER BY common_count DESC",
            nativeQuery = true)
    List<Object[]> findSimilarUsers(Long userId, List<Long> ratedProductIds);

    // ✅ User + Product로 평가 조회
    Optional<Rating> findByUserAndProduct(User user, Product product);
}
