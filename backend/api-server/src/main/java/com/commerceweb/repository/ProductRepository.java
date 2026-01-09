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

    // ✅ 평점 높은 순 정렬
    List<Product> findAllByOrderByAverageRatingDesc();

    // ✅ 조회수 높은 순 정렬
    List<Product> findAllByOrderByViewCountDesc();

    // ✅ 가격 범위 검색
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    // ✅ 카테고리별 검색
    List<Product> findByCategory(String category);

    // ✅ 이름으로 검색 (대소문자 무시)
    List<Product> findByNameContainingIgnoreCase(String name);

    // ✅ 평점 4.0 이상인 상품 조회 (인기 상품 추천용)
    @Query("SELECT p FROM Product p WHERE p.averageRating >= 4.0 ORDER BY p.averageRating DESC")
    List<Product> findHighRatedProducts();

    // ✅ ID 목록으로 상품 조회 (추천 시스템용)
    List<Product> findByIdIn(List<Long> ids);
}
