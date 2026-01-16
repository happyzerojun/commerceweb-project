package com.commerceweb.repository;

import com.commerceweb.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
