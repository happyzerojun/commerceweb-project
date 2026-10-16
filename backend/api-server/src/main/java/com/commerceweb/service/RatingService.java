package com.commerceweb.service;

import com.commerceweb.repository.RatingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추천(협업 필터링)이 읽는 ratings 테이블을 리뷰에서 파생해 유지한다 (TS-08).
 * <p>
 * 이전에는 ratings에 쓰는 경로(rateProduct)를 호출하는 컨트롤러가 없어 ratings가 늘 비어 있었고,
 * 실제 평점은 리뷰(review)로만 들어와 추천이 항상 Cold Start(인기 상품)로 빠졌다.
 * 또 rateProduct는 상품 평균을 ratings 기준으로 다시 덮어써 리뷰 기준 평균과 충돌할 수 있었다.
 * 이제 원천은 리뷰 하나이고, ratings는 (사용자, 상품)당 1행인 추천용 투영(projection)이다.
 * <p>
 * 규칙: (사용자, 상품)의 평점 = 그 사용자가 그 상품에 남긴 가장 최근 리뷰의 별점. 리뷰가 하나도 없으면 행 삭제.
 * 한 사용자가 같은 상품을 여러 번 주문해 리뷰가 여러 개일 수 있으므로 "최신 리뷰"를 현재 취향으로 본다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;

    /**
     * 리뷰 변경 직후 같은 트랜잭션 안에서 호출한다. 리뷰 쓰기와 함께 커밋·롤백된다.
     * 호출 전에 상품 행 잠금(ProductRepository.lockForRatingUpdate)을 잡아 두면
     * 같은 (사용자, 상품)의 동시 변경도 직렬화되어 최종 상태가 리뷰와 어긋나지 않는다.
     */
    @Transactional
    public void syncWithReviews(Long userId, Long productId) {
        int deleted = ratingRepository.deleteIfNoReview(userId, productId);
        int upserted = ratingRepository.upsertFromLatestReview(userId, productId);
        log.debug("ratings 동기화 userId={}, productId={}, deleted={}, upserted={}", userId, productId, deleted, upserted);
    }
}
