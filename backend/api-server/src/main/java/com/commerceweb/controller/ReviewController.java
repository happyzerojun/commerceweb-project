package com.commerceweb.controller;

import com.commerceweb.dto.ReviewDto;
import com.commerceweb.entity.User;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.service.ReviewService;
import com.commerceweb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // ✅ Spring Security가 주입해주는 인증 객체 인터페이스
import java.util.List;

/**
 * 리뷰 관리 API 컨트롤러
 * - 리뷰 작성, 조회, 수정, 삭제 기능 제공
 * - 인증된 사용자(로그인한 유저)만 작성/수정/삭제 가능
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // React 프론트엔드에서의 접근 허용
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * ✅ 리뷰 작성 API
     * POST /api/reviews
     *
     * @param principal 현재 로그인한 사용자의 인증 정보 (Spring Security가 자동 주입)
     * @param reviewDto 클라이언트가 보낸 리뷰 데이터 (상품ID, 내용, 평점 등)
     * @return 저장된 리뷰 데이터 반환
     */
    @PostMapping
    public ResponseEntity<ReviewDto> createReview(
            Principal principal, // 💡 중요: @AuthenticationPrincipal String email 대신 Principal 객체를 사용해야 안전함
            @RequestBody ReviewDto reviewDto
    ) {
        // 1. 로그인 여부 확인 (JwtFilter 통과했어도 혹시 모를 null 체크)
        if (principal == null) {
            throw new RuntimeException("로그인이 필요한 서비스입니다.");
        }

        // 2. Principal에서 이메일(Username) 추출
        // JwtFilter에서 userDetails.getUsername()을 이메일로 설정했으므로 여기서 이메일이 나옴
        String email = principal.getName();

        // 3. 이메일로 실제 DB User 엔티티 조회
        User user = userService.findByEmail(email);

        // 4. 리뷰 DTO에 작성자 ID(userId) 주입
        // 프론트엔드에서 userId를 보내지 않아도, 토큰 정보로 서버에서 직접 할당하므로 보안상 안전함
        reviewDto.setUserId(user.getId());

        // 5. 서비스 호출하여 리뷰 저장
        return ResponseEntity.ok(reviewService.createReview(reviewDto));
    }

    /**
     * 특정 상품의 리뷰 목록 조회 API (인증 불필요)
     * GET /api/reviews/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable Long productId) {
        // 로그인 안 한 사람도 상품 리뷰는 볼 수 있어야 하므로 인증 체크 없음
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    /**
     * 전체 리뷰 조회 API (인증 불필요)
     * GET /api/reviews
     */
    @GetMapping
    public ResponseEntity<List<ReviewDto>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    /**
     * ✅ 리뷰 수정 API
     * PUT /api/reviews/{reviewId}
     * - 본인이 작성한 리뷰만 수정 가능하도록 서비스 계층에서 검증 로직이 포함됨
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewDto reviewDto,
            Principal principal // ✅ Authentication 객체 대신 더 범용적인 Principal 사용
    ) {
        try {
            // 1. 로그인 체크
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }

            // 2. 현재 로그인한 유저의 이메일 가져오기
            String email = principal.getName();

            // 3. DB에서 해당 유저 조회 (Optional 처리)
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

            // 4. 서비스 호출 (리뷰 수정)
            // 서비스 내부에서 '리뷰 작성자'와 '현재 요청한 유저(user.getId())'가 일치하는지 검사함
            ReviewDto updatedReview = reviewService.updateReview(
                    reviewId,
                    reviewDto.getContent(),
                    reviewDto.getRating(),
                    user.getId() // 요청자 ID 전달
            );

            return ResponseEntity.ok(updatedReview);

        } catch (Exception e) {
            // 본인 리뷰가 아니거나 에러 발생 시 403 Forbidden 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * ✅ 리뷰 삭제 API
     * DELETE /api/reviews/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            Principal principal, // ✅ 안전한 방식
            @PathVariable Long reviewId
    ) {
        // 1. 로그인 체크
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        // 2. 사용자 정보 조회
        String email = principal.getName();
        User user = userService.findByEmail(email);

        // 3. 서비스 호출 (리뷰 삭제)
        // 마찬가지로 서비스 내부에서 본인 확인 후 삭제 진행
        reviewService.deleteReview(reviewId, user);

        // 4. 성공 시 200 OK (본문 없음) 반환
        return ResponseEntity.ok().build();
    }
}
