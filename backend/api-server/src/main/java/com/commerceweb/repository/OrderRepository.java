package com.commerceweb.repository;

import com.commerceweb.entity.Order;
import com.commerceweb.entity.User;
import com.commerceweb.dto.SalesStatDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByOrderDateDesc(User user);

    @Query("SELECT new com.commerceweb.dto.SalesStatDto(p.name, CAST(SUM(o.totalPrice) AS long), CAST(SUM(o.quantity) AS long), COALESCE(p.averageRating, 0.0)) FROM Order o JOIN o.product p GROUP BY p.name, p.averageRating")
    List<SalesStatDto> getSalesStatistics();

    // 필드명이 user와 product이므로 이에 맞게 이름을 수정합니다.
    boolean existsByUserIdAndProductIdAndStatus(Long userId, Long productId, String status);
}