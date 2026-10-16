package com.commerceweb.service;


import com.commerceweb.dto.ReviewDto;
import com.commerceweb.entity.*;
import com.commerceweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    /** Review 엔티티의 주문당 1건 유니크 제약 이름 */
    static final String UK_REVIEW_ORDER = "uk_review_order";
    static final String DUPLICATE_REVIEW_MESSAGE = "이미 이 주문에 대한 리뷰를 작성하셨습니다.";

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final RatingService ratingService;

    /*
     * 추천 캐시 무효화 (TS-08): 리뷰 작성·수정·삭제 = 협업 필터링 입력(ratings) 변경.
     * allEntries인 이유
     *  - 작성자 본인뿐 아니라 "작성자와 취향이 비슷한 다른 사용자"의 추천도 바뀐다(작성자의 평점이 그들의 추천 근거).
     *  - 캐시 키가 userId_limit 이라 한 사용자 키만 골라 지우려 해도 limit 조합을 알 수 없다.
     *  트레이드오프: 리뷰 쓰기가 잦으면 캐시 적중률이 떨어진다 → 그때는 사용자별 버전 키나 TTL만 쓰는 방식으로 전환.
     * 제거 시점: RedisCacheConfig에서 transactionAware로 설정해 커밋 후에 지운다(롤백되면 지우지 않음).
     */
    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public ReviewDto createReview(ReviewDto reviewDto) {
        validateRating(reviewDto.getRating());
        // 1. 주문 정보 조회
        Order order = orderRepository.findById(reviewDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문 내역을 찾을 수 없습니다."));

        // 2. 권한 확인 (테이블 데이터의 buyer_id와 현재 요청 userId 비교) → 403
        if (!order.getUser().getId().equals(reviewDto.getUserId())) {
            throw new AccessDeniedException("본인의 주문 내역에 대해서만 리뷰를 남길 수 있습니다.");
        }

        // 3. 중복 리뷰 체크 → 409. 이전에는 RuntimeException이라 500으로 나갔다
        //    (빠른 실패용 사전 검사일 뿐, 최종 보장은 DB 유니크 제약 uk_review_order)
        if (reviewRepository.existsByOrderId(reviewDto.getOrderId())) {
            throw new IllegalStateException(DUPLICATE_REVIEW_MESSAGE);
        }

        // 4. 리뷰 엔티티 빌드 및 저장
        Review review = Review.builder()
                .order(order)
                .product(order.getProduct()) // DB 테이블 상 productId 자동 매칭
                .user(order.getUser())       // DB 테이블 상 buyer_id 자동 매칭
                .content(reviewDto.getContent())
                .rating(reviewDto.getRating())
                .build();

        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 두 건이 모두 existsByOrderId를 통과하면 두 번째 INSERT가 유니크 제약에 걸린다.
            // 그 경우만 409 + 중복 메시지로 바꾸고, 다른 제약 위반은 중복으로 뭉개지 않고 그대로 올린다 (TS-06)
            if (isDuplicateOrderReview(e)) {
                throw new IllegalStateException(DUPLICATE_REVIEW_MESSAGE, e);
            }
            throw e;
        }

        // 5. 상품 평균 별점 갱신 (DB 집계 UPDATE) + 추천용 ratings 동기화
        afterReviewChanged(order.getUser().getId(), order.getProduct().getId());

        return convertToDto(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByProduct(Long productId) {
        // 상품 존재 여부 확인 전용 쿼리
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        return reviewRepository.findByProductOrderByCreatedAtDesc(product)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * 리뷰가 생기거나 바뀌거나 지워진 뒤 상품 평점을 다시 계산한다.
     * 이전 구현은 리뷰 목록을 읽어 자바에서 평균을 내고 product.save()로 덮어써서, 같은 상품에 리뷰가 동시에 달리면
     * 나중에 커밋한 쪽이 상대 리뷰가 빠진 평균으로 덮었다(Lost Update). (TS-10)
     * 1) 상품 행 잠금으로 같은 상품의 재계산을 직렬화 → 2) 추천용 ratings 동기화(TS-08)
     * → 3) DB가 AVG/COUNT를 직접 집계해 한 문장으로 갱신. 반올림(소수 1자리)은 ROUND(AVG(rating), 1)로 DB에서 한다.
     */
    private void afterReviewChanged(Long userId, Long productId) {
        productRepository.lockForRatingUpdate(productId);
        ratingService.syncWithReviews(userId, productId);
        productRepository.refreshRatingStats(productId);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getAllReviews() {
        // 모든 리뷰를 최신순으로 조회
        return reviewRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ReviewDto convertToDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName()) // ✅ 상품명 매핑
                .orderId(review.getOrder().getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .build();
    }

    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("본인의 리뷰만 삭제할 수 있습니다.");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        reviewRepository.flush();   // 잠금·집계 전에 DELETE를 DB에 반영해 잠금 순서를 리뷰 행 → 상품 행으로 고정
        // 삭제 후에도 상품 평균 평점·리뷰 수를 다시 계산한다 (이전에는 삭제해도 평점이 그대로 남았다)
        afterReviewChanged(user.getId(), productId);
    }

    static boolean isDuplicateOrderReview(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
                return UK_REVIEW_ORDER.equalsIgnoreCase(cve.getConstraintName());
            }
        }
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(UK_REVIEW_ORDER);
    }

    /** 별점은 1~5 정수만 허용 (이전에는 범위 검증이 없어 100점도 저장됐다) */
    private static void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이 정수여야 합니다.");
        }
    }

    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public ReviewDto updateReview(Long reviewId, String content, Integer rating, Long currentUserId) {
        validateRating(rating);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        // ✅ 중요: 리뷰 작성자의 ID와 현재 로그인한 유저의 ID 비교
        if (!review.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        review.setContent(content);
        review.setRating(rating);
        reviewRepository.saveAndFlush(review);   // 집계가 바뀐 별점을 보도록 먼저 반영

        // 별점 갱신 (DB 집계 UPDATE)
        afterReviewChanged(currentUserId, review.getProduct().getId());

        return convertToDto(review);
    }
}