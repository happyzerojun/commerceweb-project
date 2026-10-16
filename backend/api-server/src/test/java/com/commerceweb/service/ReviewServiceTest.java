package com.commerceweb.service;

import com.commerceweb.dto.ReviewDto;
import com.commerceweb.entity.Order;
import com.commerceweb.entity.Product;
import com.commerceweb.entity.Review;
import com.commerceweb.entity.User;
import com.commerceweb.repository.OrderRepository;
import com.commerceweb.repository.ProductRepository;
import com.commerceweb.repository.ReviewRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("리뷰 서비스 테스트")
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock ProductRepository productRepository;
    @Mock OrderRepository orderRepository;

    @InjectMocks ReviewService reviewService;

    User buyer;
    Product product;
    Order order;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        buyer = User.builder().id(1L).name("buyer").build();
        product = Product.builder().id(100L).name("p").build();
        order = Order.builder().id(10L).user(buyer).product(product).build();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReviewDto request(long userId) {
        return ReviewDto.builder().orderId(10L).userId(userId).content("good").rating(5).build();
    }

    @Test
    @DisplayName("이미 리뷰가 있는 주문이면 IllegalStateException(409)")
    void duplicatePreCheckIsConflict() {
        when(reviewRepository.existsByOrderId(10L)).thenReturn(true);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> reviewService.createReview(request(1L)));
        assertEquals(ReviewService.DUPLICATE_REVIEW_MESSAGE, e.getMessage());
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("사전 검사를 동시에 통과해 유니크 제약에 걸려도 409로 바꾼다")
    void uniqueViolationIsTranslatedToConflict() {
        when(reviewRepository.existsByOrderId(10L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenThrow(new DataIntegrityViolationException("dup",
                new ConstraintViolationException("dup", new SQLException("dup", "23505"), "uk_review_order")));

        assertThrows(IllegalStateException.class, () -> reviewService.createReview(request(1L)));
    }

    @Test
    @DisplayName("다른 제약 위반은 중복 리뷰로 뭉개지 않고 그대로 던진다")
    void otherConstraintViolationIsNotMaskedAsDuplicate() {
        when(reviewRepository.existsByOrderId(10L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenThrow(new DataIntegrityViolationException("null",
                new ConstraintViolationException("null", new SQLException("null", "23502"), "review_content_not_null")));

        assertThrows(DataIntegrityViolationException.class, () -> reviewService.createReview(request(1L)));
    }

    @Test
    @DisplayName("남의 주문에는 리뷰를 쓸 수 없다 (403)")
    void otherUsersOrderIsForbidden() {
        assertThrows(AccessDeniedException.class, () -> reviewService.createReview(request(2L)));
    }
}
