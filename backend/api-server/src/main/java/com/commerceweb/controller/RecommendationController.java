package com.commerceweb.controller;

import java.util.List;

import com.commerceweb.dto.product.ProductResponse;
import com.commerceweb.dto.product.RecommendationResponse;
import com.commerceweb.entity.User;
import com.commerceweb.repository.UserRepository;
import com.commerceweb.service.RecommendationService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping("/recommendations")
    public RecommendationResponse getRecommendations(@RequestParam(defaultValue = "5") int topN) {
        Long userId = getCurrentUserId();

        List<ProductResponse> products = recommendationService.getRecommendations(userId, topN).stream()
                .map(ProductResponse::from)
                .toList();

        return new RecommendationResponse(products);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // 보통 JWT subject가 여기로 들어옴

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        return user.getId();
    }
}