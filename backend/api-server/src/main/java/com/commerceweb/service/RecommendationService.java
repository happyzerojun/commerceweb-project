// backend/src/main/java/com/commerceweb/service/RecommendationService.java

package com.commerceweb.service;

import com.commerceweb.entity.Product;
import com.commerceweb.entity.Rating;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;

    /**
     * 협력 필터링 기반 추천
     * <p>
     * 알고리즘:
     * 1. 현재 사용자가 평가한 상품 목록 조회
     * 2. 같은 상품을 평가한 유사 사용자 찾기
     * 3. 유사 사용자들이 높게 평가한 상품 중 현재 사용자가 평가하지 않은 상품 추천
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", key = "#userId")
    public List<Product> getRecommendations(Long userId, int limit) {
        log.info("🔍 추천 시스템 시작: userId={}", userId);

        // 1. 사용자가 평가한 상품 ID 목록
        List<Long> ratedProductIds = ratingRepository.findRatedProductIdsByUserId(userId);

        if (ratedProductIds.isEmpty()) {
            log.info("📌 평가 기록 없음 → 인기 상품 추천");
            return productRepository.findHighRatedProducts()
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // 2. 유사 사용자 찾기 (공통 평가 상품 개수로 유사도 측정)
        List<Object[]> similarUsers = ratingRepository.findSimilarUsers(userId, ratedProductIds);

        if (similarUsers.isEmpty()) {
            log.info("📌 유사 사용자 없음 → 인기 상품 추천");
            return productRepository.findHighRatedProducts()
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // 3. 유사 사용자들의 평점 수집
        Map<Long, Double> productScores = new HashMap<>();

        // 상위 5명의 유사 사용자만 사용
        int similarUserCount = Math.min(5, similarUsers.size());

        for (int i = 0; i < similarUserCount; i++) {
            Long similarUserId = (Long) similarUsers.get(i)[0];

            List<Rating> ratings = ratingRepository.findByUserId(similarUserId);

            for (Rating rating : ratings) {
                Long productId = rating.getProduct().getId();

                // 이미 평가한 상품은 제외
                if (ratedProductIds.contains(productId)) {
                    continue;
                }

                // 가중 평균 계산
                productScores.merge(
                        productId,
                        rating.getScore().doubleValue(),
                        (oldVal, newVal) -> (oldVal + newVal) / 2
                );
            }
        }

        // 4. 점수 순으로 정렬하여 추천
        List<Long> recommendedProductIds = productScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Product> recommendations = productRepository.findByIdIn(recommendedProductIds);

        log.info("✅ 추천 완료: {} 개 상품", recommendations.size());

        return recommendations;
    }

    /**
     * 카테고리 기반 추천 (보조 알고리즘)
     */
    @Transactional(readOnly = true)
    public List<Product> getRecommendationsByCategory(String category, int limit) {
        return productRepository.findByCategory(category)
                .stream()
                .sorted(Comparator.comparing(Product::getAverageRating).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Product> getPopularProducts(int limit) {
        log.info("🔥 인기 상품 조회");

        return productRepository.findHighRatedProducts()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}