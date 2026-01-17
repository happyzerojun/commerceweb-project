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

/**
 * 상품 추천 비즈니스 로직을 처리하는 서비스 클래스
 * 주요 기능:
 * 1. 사용자 기반 협업 필터링 (User-Based Collaborative Filtering)
 * 2. 카테고리별 추천 (Content-Based Filtering의 일종)
 * 3. 인기 상품 조회 (Cold Start 문제 해결용)
 * [성능 최적화]
 * - Redis 캐싱으로 동일 사용자의 반복 요청 시간 단축
 * - 읽기 전용 트랜잭션으로 DB 최적화
 * - 상위 5명의 유사 사용자만 분석 (시간 복잡도 감소)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;

    /**
     * 협업 필터링(Collaborative Filtering) 기반 추천 메서드
     * 사용자의 과거 평점 데이터를 분석하여 취향이 비슷한 다른 사용자가 선호하는 상품을 추천합니다.
     *
     * <p>알고리즘 동작 원리:</p>
     * <ol>
     *   <li>현재 사용자가 평가한 상품 목록 조회 (예: 상품 1,2,3에 평가함)</li>
     *   <li>같은 상품들을 평가한 다른 사용자 탐색 (공통 평가 이력이 있는 사용자)</li>
     *   <li>유사 사용자들이 높은 점수를 준 상품 중, 현재 사용자가 아직 평가하지 않은 상품 선별</li>
     *   <li>각 상품의 예상 평점 계산 (유사 사용자들의 평점 평균)</li>
     *   <li>높은 점수 순으로 정렬하여 상위 N개 반환</li>
     * </ol>
     *
     * @param userId 추천을 받을 사용자의 ID
     * @param limit 반환할 추천 상품의 최대 개수
     * @return 협업 필터링으로 계산된 추천 상품 리스트
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", key = "#userId + '_' + #limit")
    public List<Product> getRecommendations(Long userId, int limit) {
        log.info("================================================================================");
        log.info("🔍 협업 필터링 추천 시작");
        log.info("================================================================================");
        log.info("📊 요청 파라미터 - userId: {}, limit: {}", userId, limit);

        // ==========================================
        // 입력값 검증 (Validation)
        // ==========================================
        try {
            if (userId == null || userId <= 0) {
                log.error("❌ 유효하지 않은 userId: {}", userId);
                throw new IllegalArgumentException("userId는 양수여야 합니다. 입력값: " + userId);
            }

            if (limit <= 0 || limit > 100) {
                log.warn("⚠️ limit 값이 범위를 벗어남. 입력값: {}, 기본값 5로 조정", limit);
                limit = Math.min(Math.max(limit, 1), 100);
            }

            log.info("✅ 입력값 검증 통과");
        } catch (Exception e) {
            log.error("❌ 입력값 검증 중 에러 발생: {}", e.getMessage());
            throw e;
        }

        // ==========================================
        // 1단계: 현재 사용자의 평가 이력 조회
        // ==========================================
        log.info("📍 1단계: 사용자 평가 이력 조회 시작...");
        List<Long> ratedProductIds;

        try {
            long startTime = System.currentTimeMillis();
            ratedProductIds = ratingRepository.findRatedProductIdsByUserId(userId);
            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ 사용자 평가 이력 조회 완료 ({}ms)", duration);
            log.info("📊 현재 사용자가 평가한 상품 수: {}", ratedProductIds.size());

            if (!ratedProductIds.isEmpty()) {
                log.debug("평가한 상품 ID 목록: {}", ratedProductIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")));
            }
        } catch (Exception e) {
            log.error("❌ 1단계 실패 - 사용자 평가 이력 조회 에러: {}", e.getMessage());
            log.error("스택 트레이스: ", e);
            return getPopularProducts(limit);
        }

        // [Cold Start 문제 대응]
        if (ratedProductIds.isEmpty()) {
            log.info("📌 Cold Start 상황 감지: 평가 기록 없음(신규 유저)");
            log.info("→ 인기 상품 추천으로 대체");
            return getPopularProducts(limit);
        }

        // ==========================================
        // 2단계: 유사 사용자 탐색
        // ==========================================
        log.info("📍 2단계: 유사 사용자 탐색 시작...");
        List<Object[]> similarUsers;

        try {
            long startTime = System.currentTimeMillis();
            similarUsers = ratingRepository.findSimilarUsers(userId, ratedProductIds);
            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ 유사 사용자 탐색 완료 ({}ms)", duration);
            log.info("📊 발견된 유사 사용자 수: {}", similarUsers.size());

            if (!similarUsers.isEmpty()) {
                log.debug("유사 사용자 목록 (상위 5개):");
                for (int i = 0; i < Math.min(5, similarUsers.size()); i++) {
                    Long uId = (Long) similarUsers.get(i)[0];
                    Long commonCount = (Long) similarUsers.get(i)[1];
                    log.debug("  - userId: {}, 공통 평가 수: {}", uId, commonCount);
                }
            }
        } catch (Exception e) {
            log.error("❌ 2단계 실패 - 유사 사용자 탐색 에러: {}", e.getMessage());
            log.error("스택 트레이스: ", e);
            return getPopularProducts(limit);
        }

        // 유사한 취향의 사용자가 없는 경우
        if (similarUsers.isEmpty()) {
            log.info("📌 유사 사용자 부재: 추천할 유사 사용자가 없습니다");
            log.info("→ 인기 상품 추천으로 대체");
            return getPopularProducts(limit);
        }

        // ==========================================
        // 3단계: 유사 사용자들의 평점 데이터 수집 및 예상 평점 계산
        // ==========================================
        log.info("📍 3단계: 유사 사용자 평점 데이터 수집 및 분석 시작...");
        Map<Long, Double> productScores = new HashMap<>();
        int similarUserCount = Math.min(5, similarUsers.size());

        log.info("분석할 유사 사용자 수 (상위 K=5): {}", similarUserCount);

        try {
            for (int i = 0; i < similarUserCount; i++) {
                Long similarUserId = (Long) similarUsers.get(i)[0];
                Long commonCount = (Long) similarUsers.get(i)[1];

                log.debug("유사 사용자 분석 중 ({}/{}): userId={}, 공통 평가 수={}",
                        i + 1, similarUserCount, similarUserId, commonCount);

                long startTime = System.currentTimeMillis();
                List<Rating> ratings = ratingRepository.findByUserId(similarUserId);
                long duration = System.currentTimeMillis() - startTime;

                log.debug("  ✅ 평점 데이터 조회 완료 ({}ms) - {} 개 상품 평가", duration, ratings.size());

                int productsAdded = 0;
                for (Rating rating : ratings) {
                    Long productId = rating.getProduct().getId();
                    Double ratingScore = rating.getScore().doubleValue();

                    // 이미 평가한 상품 제외
                    if (ratedProductIds.contains(productId)) {
                        log.debug("    - 스킵 (이미 평가): productId={}", productId);
                        continue;
                    }

                    // 점수 계산
                    Double previousScore = productScores.get(productId);
                    productScores.merge(
                            productId,
                            ratingScore,
                            (oldVal, newVal) -> (oldVal + newVal) / 2
                    );

                    if (previousScore == null) {
                        log.debug("    - 신규 상품 추가: productId={}, 평점={}", productId, ratingScore);
                    }
                    productsAdded++;
                }
                log.debug("  📊 해당 사용자로부터 {} 개 상품 점수 업데이트", productsAdded);
            }
            log.info("✅ 3단계 완료 - 총 {} 개의 추천 후보 상품 발견", productScores.size());
        } catch (Exception e) {
            log.error("❌ 3단계 실패 - 평점 데이터 수집 에러: {}", e.getMessage());
            log.error("스택 트레이스: ", e);
            return getPopularProducts(limit);
        }

        // ==========================================
        // 4단계: 예상 평점으로 정렬 및 최종 상품 리스트 변환
        // ==========================================
        log.info("📍 4단계: 예상 평점으로 정렬 및 최종 상품 조회 시작...");
        List<Long> recommendedProductIds;

        try {
            long startTime = System.currentTimeMillis();

            // 예상 평점 기준 정렬
            recommendedProductIds = productScores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ 정렬 완료 ({}ms) - {} 개 상품 선정", duration, recommendedProductIds.size());
            log.debug("최종 추천 상품 ID 목록: {}", recommendedProductIds);

            // 상품 상세 정보 조회
            long dbStartTime = System.currentTimeMillis();
            List<Product> recommendations = productRepository.findByIdIn(recommendedProductIds);
            long dbDuration = System.currentTimeMillis() - dbStartTime;

            log.info("✅ 상품 상세 정보 조회 완료 ({}ms) - {} 개", dbDuration, recommendations.size());

            // 최종 검증
            if (recommendations.isEmpty()) {
                log.warn("⚠️ 경고: 추천할 상품이 없습니다 (DB에서 조회 실패)");
                return getPopularProducts(limit);
            }

            // 최종 로그
            log.info("================================================================================");
            log.info("✅ 협업 필터링 추천 성공!");
            log.info("================================================================================");
            log.info("🎁 최종 추천 상품 목록:");
            for (int i = 0; i < recommendations.size(); i++) {
                Product product = recommendations.get(i);
                log.info("  {}. [ID:{}] {} - 평점: {}",
                        i + 1, product.getId(), product.getName(),
                        String.format("%.2f", product.getAverageRating()));
            }
            log.info("================================================================================");

            return recommendations;
        } catch (Exception e) {
            log.error("❌ 4단계 실패 - 최종 변환 에러: {}", e.getMessage());
            log.error("스택 트레이스: ", e);
            return getPopularProducts(limit);
        }
    }

    /**
     * 카테고리 기반 추천 (보조 알고리즘)
     * 특정 카테고리 내에서 평점이 높은 상품들을 추천합니다.
     * 사용 사례:
     * - 카테고리 페이지의 "인기 상품" 섹션
     * - 사용자가 특정 카테고리를 클릭했을 때의 연관 상품 추천
     * - 협업 필터링이 작동하지 않을 때의 대체 로직
     *
     * @param category 조회할 카테고리명 (예: "의류", "전자제품")
     * @param limit 반환 개수 (보통 5~10개)
     * @return 해당 카테고리 내 평점이 높은 상품 리스트
     */
    @Transactional(readOnly = true)
    public List<Product> getRecommendationsByCategory(String category, int limit) {
        log.info("📂 카테고리 기반 추천 시작: category={}, limit={}", category, limit);

        try {
            if (category == null || category.trim().isEmpty()) {
                log.error("❌ 유효하지 않은 카테고리명: {}", category);
                return new ArrayList<>();
            }

            long startTime = System.currentTimeMillis();
            List<Product> results = productRepository.findByCategory(category)
                    .stream()
                    .sorted(Comparator.comparing(Product::getAverageRating).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ 카테고리 추천 완료 ({}ms) - {} 개 상품", duration, results.size());
            return results;
        } catch (Exception e) {
            log.error("❌ 카테고리 추천 에러: {}", e.getMessage());
            log.error("스택 트레이스: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * 인기 상품 조회 (전체 랭킹)
     * 모든 카테고리를 통틀어 평점이 가장 높은 상품들을 반환합니다.
     * 사용 사례:
     * - 메인 페이지의 '베스트 셀러' 섹션
     * - Cold Start 문제 해결 (신규 사용자 대상)
     * - 추천 시스템의 폴백 로직 (유사 사용자가 없을 때)
     *
     * @param limit 반환 개수 (보통 10~20개)
     * @return 전체 상품 중 평점이 높은 상품 리스트
     */
    @Transactional(readOnly = true)
    public List<Product> getPopularProducts(int limit) {
        log.info("🔥 인기 상품 조회 시작: limit={}", limit);

        try {
            long startTime = System.currentTimeMillis();
            List<Product> results = productRepository.findHighRatedProducts()
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ 인기 상품 조회 완료 ({}ms) - {} 개 상품", duration, results.size());
            for (int i = 0; i < results.size(); i++) {
                Product product = results.get(i);
                log.debug("  {}. [ID:{}] {} - 평점: {}",
                        i + 1, product.getId(), product.getName(),
                        String.format("%.2f", product.getAverageRating()));
            }

            return results;
        } catch (Exception e) {
            log.error("❌ 인기 상품 조회 에러: {}", e.getMessage());
            log.error("스택 트레이스: ", e);
            return new ArrayList<>();
        }
    }
}
