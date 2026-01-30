package org.cooking.cookingbenefits.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "recipes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cooking_steps", nullable = false, columnDefinition = "TEXT")
    private String cookingSteps;

    @Column(name = "cooking_time_minutes")
    private Integer cookingTimeMinutes;

    @Column(nullable = false)
    @Builder.Default
    private String difficulty = "medium";

    @Column(nullable = false)
    @Builder.Default
    private Integer servings = 2;

    private String category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RecipeIngredient> ingredients = new HashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserFavorite> favoritedByUsers = new HashSet<>();
}