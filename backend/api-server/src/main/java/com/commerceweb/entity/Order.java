package com.commerceweb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Integer quantity;

    // name을 total_amount에서 total_price로 변경 (DB 표준에 맞춤)
    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    private String status;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    private LocalDateTime orderDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.orderDate = now;
        this.createdAt = now;
        this.updatedAt = now;

        // @PrePersist에서 확실하게 기본값 할당
        if (this.status == null) this.status = "PAID";
        if (this.shippingAddress == null) this.shippingAddress = "기본 배송지";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}