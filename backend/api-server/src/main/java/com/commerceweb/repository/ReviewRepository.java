package com.commerceweb.repository;

import com.commerceweb.entity.Review;
import com.commerceweb.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 특정 상품에 달린 리뷰들을 최신순으로 가져오기
    List<Review> findByProductOrderByCreatedAtDesc(Product product);

    // 특정 상품의 리뷰 개수 세기
    long countByProductId(Long productId);

    boolean existsByOrderId(Long orderId);
}