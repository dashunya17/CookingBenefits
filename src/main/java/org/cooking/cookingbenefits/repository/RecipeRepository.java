package org.cooking.cookingbenefits.repository;

import org.cooking.cookingbenefits.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByIsApprovedTrue();
    List<Recipe> findByTitleContainingIgnoreCase(String title);
    @Query("SELECT DISTINCT r FROM Recipe r " +
            "JOIN r.ingredients ri " +
            "WHERE r.isApproved = true " +
            "AND ri.product.id IN :productIds " +
            "GROUP BY r " +
            "HAVING COUNT(DISTINCT ri.product.id) >= :minIngredients")
    List<Recipe> findRecipesByProducts(@Param("productIds") List<Long> productIds,
                                       @Param("minIngredients") int minIngredients);

    @Query("SELECT r FROM Recipe r WHERE r.isApproved = true")
    List<Recipe> findApprovedRecipes();
}