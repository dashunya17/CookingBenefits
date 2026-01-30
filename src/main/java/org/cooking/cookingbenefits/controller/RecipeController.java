package org.cooking.cookingbenefits.controller;

import lombok.RequiredArgsConstructor;
import org.cooking.cookingbenefits.dto.RecipeDTO;
import org.cooking.cookingbenefits.entity.User;
import org.cooking.cookingbenefits.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/recommended")
    public ResponseEntity<List<RecipeDTO>> getRecommendedRecipes(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") int limit) {

        List<RecipeDTO> recommendations = recipeService.getRecommendedRecipes(user.getId(), limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/search")
    public ResponseEntity<List<RecipeDTO>> searchRecipes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(defaultValue = "0") int minIngredients) {

        // Реализация поиска
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDTO> getRecipeById(@PathVariable Long id) {
        // Получение деталей рецепта
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    @PostMapping("/{recipeId}/favorite")
    public ResponseEntity<Void> addToFavorites(
            @AuthenticationPrincipal User user,
            @PathVariable Long recipeId) {

        recipeService.addToFavorites(user.getId(), recipeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{recipeId}/favorite")
    public ResponseEntity<Void> removeFromFavorites(
            @AuthenticationPrincipal User user,
            @PathVariable Long recipeId) {

        recipeService.removeFromFavorites(user.getId(), recipeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<RecipeDTO>> getFavorites(@AuthenticationPrincipal User user) {
        List<RecipeDTO> favorites = recipeService.getUserFavorites(user.getId());
        return ResponseEntity.ok(favorites);
    }
}
