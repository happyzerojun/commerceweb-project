package com.commerceweb.controller;

import com.commerceweb.dto.ReviewDto;
import com.commerceweb.entity.User;
import com.commerceweb.service.ReviewService;
import com.commerceweb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.commerceweb.repository.UserRepository; // Repository 주입 필요
import org.springframework.security.core.Authentication;

import java.nio.file.AccessDeniedException;
import com.commerceweb.entity.User; // 본인이 만든 User 엔티티
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final UserRepository userRepository; // 생성자 주입 확인

    // ✅ 리뷰 작성: 토큰 정보를 사용하여 userId를 서버에서 직접 주입
    @PostMapping
    public ResponseEntity<ReviewDto> createReview(
            @AuthenticationPrincipal String userEmail,
            @RequestBody ReviewDto reviewDto
    ) {
        // 토큰에 담긴 이메일로 실제 User를 찾아서 ID를 세팅 (프론트엔드 null 전송 대응)
        User user = userService.findByEmail(userEmail);
        reviewDto.setUserId(user.getId());

        return ResponseEntity.ok(reviewService.createReview(reviewDto));
    }

    // 특정 상품의 리뷰 목록 조회
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    // 전체 상품 조회
    @GetMapping
    public ResponseEntity<List<ReviewDto>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewDto reviewDto,
            Authentication authentication // ✅ Object principal 대신 이걸 사용하세요
    ) {
        try {
            // 1. 현재 로그인한 유저의 이메일(Username) 가져오기
            String email = authentication.getName();

            // 2. DB에서 해당 유저 찾기
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

            // 3. 서비스 호출 (찾은 유저의 ID 전달)
            ReviewDto updatedReview = reviewService.updateReview(
                    reviewId,
                    reviewDto.getContent(),
                    reviewDto.getRating(),
                    user.getId()
            );

            return ResponseEntity.ok(updatedReview);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long reviewId
    ) {
        User user = userService.findByEmail(userEmail);
        reviewService.deleteReview(reviewId, user);
        return ResponseEntity.ok().build();
    }
}