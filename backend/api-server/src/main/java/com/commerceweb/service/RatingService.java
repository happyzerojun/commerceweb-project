package com.commerceweb.service;

import com.commerceweb.entity.Product;
import com.commerceweb.entity.Rating;
import com.commerceweb.entity.User;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.RatingRepository;
import com.commerceweb.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 상품 평가 생성 또는 업데이트
     */
    @Transactional
    @CacheEvict(value = "recommendations", key = "#userId")
    public Rating rateProduct(Long userId, Long productId, Integer score, String review) {
        log.info("⭐ 상품 평가: userId={}, productId={}, score={}", userId, productId, score);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));

        // 기존 평가 조회
        Optional<Rating> existingRating = ratingRepository.findByUserAndProduct(user, product);

        Rating rating;
        if (existingRating.isPresent()) {
            rating = existingRating.get();
            rating.setScore(score);
            rating.setReview(review);
            log.info("📝 평가 업데이트");
        } else {
            rating = Rating.builder()
                    .user(user)
                    .product(product)
                    .score(score)
                    .review(review)
                    .build();
            log.info("✨ 새 평가 생성");
        }

        Rating savedRating = ratingRepository.save(rating);
        updateProductRating(product);

        return savedRating;
    }

    /**
     * 평가 조회
     */
    @Transactional(readOnly = true)
    public Rating getRating(Long id) {
        return ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("평가를 찾을 수 없습니다"));
    }

    /**
     * 평가 삭제
     */
    @Transactional
    @CacheEvict(value = "recommendations", key = "#userId")
    public void deleteRating(Long userId, Long ratingId) {
        log.info("🗑️ 평가 삭제");

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("평가를 찾을 수 없습니다"));

        Product product = rating.getProduct();
        ratingRepository.deleteById(ratingId);
        updateProductRating(product);
    }

    /**
     * 상품의 평점 및 평가 수 업데이트
     */
    @Transactional
    private void updateProductRating(Product product) {
        java.util.List<Rating> ratings = ratingRepository.findByProductId(product.getId());

        if (ratings.isEmpty()) {
            product.setAverageRating(0.0);
            product.setRatingCount(0L);
        } else {
            double averageRating = ratings.stream()
                    .mapToInt(Rating::getScore)
                    .average()
                    .orElse(0.0);

            product.setAverageRating(averageRating);
            product.setRatingCount((long) ratings.size());
        }

        productRepository.save(product);
    }
}
