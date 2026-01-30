package org.cooking.cookingbenefits.repository;

import org.cooking.cookingbenefits.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    List<UserFavorite> findByUserId(Long userId);
    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);
    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);
}