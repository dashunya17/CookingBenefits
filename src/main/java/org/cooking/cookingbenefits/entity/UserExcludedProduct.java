package org.cooking.cookingbenefits.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_excluded_products",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserExcludedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @CreationTimestamp
    @Column(name = "excluded_at", updatable = false)
    private LocalDateTime excludedAt;
}