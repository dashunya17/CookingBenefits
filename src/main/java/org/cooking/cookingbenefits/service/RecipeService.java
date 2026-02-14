package org.cooking.cookingbenefits.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cooking.cookingbenefits.dto.RecipeDTO;
import org.cooking.cookingbenefits.entity.*;
import org.cooking.cookingbenefits.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserProductRepository userProductRepository;
    private final UserExcludedProductRepository userExclusionRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final ProductRepository productRepository;

    public List<RecipeDTO> getRecommendedRecipes(Long userId, int limit) {
        try {
            log.info("Получение рекомендаций для пользователя: {}", userId);

            List<Recipe> allRecipes = recipeRepository.findByIsApprovedTrue();

            if (allRecipes.isEmpty()) {
                log.warn("Нет одобренных рецептов в базе");
                return new ArrayList<>();
            }

            Set<Long> availableProductIds = userProductRepository.findByUserId(userId).stream()
                    .map(up -> up.getProduct().getId())
                    .collect(Collectors.toSet());

            Set<Long> excludedProductIds = userExclusionRepository.findByUserId(userId).stream()
                    .map(ue -> ue.getProduct().getId())
                    .collect(Collectors.toSet());

            List<RecipeDTO> recommendations = new ArrayList<>();

            for (Recipe recipe : allRecipes) {
                try {
                    RecipeDTO dto = convertToDTO(recipe);

                    // Рассчитываем процент совпадения
                    List<RecipeIngredient> ingredients = recipeIngredientRepository.findByRecipeId(recipe.getId());
                    int totalIngredients = ingredients.size();
                    int availableCount = 0;
                    int excludedCount = 0;

                    for (RecipeIngredient ingredient : ingredients) {
                        Long productId = ingredient.getProduct().getId();
                        if (excludedProductIds.contains(productId)) {
                            excludedCount++;
                        } else if (availableProductIds.contains(productId)) {
                            availableCount++;
                        }
                    }

                    double matchPercentage = 0;
                    if (totalIngredients > 0) {
                        matchPercentage = (double) availableCount / totalIngredients * 100;
                        if (excludedCount > 0) {
                            matchPercentage *= 0.5; // Штраф за исключенные
                        }
                    }

                    dto.setMatchPercentage(Math.round(matchPercentage * 10) / 10.0);
                    dto.setIsFavorite(userFavoriteRepository.existsByUserIdAndRecipeId(userId, recipe.getId()));

                    recommendations.add(dto);
                } catch (Exception e) {
                    log.error("Ошибка обработки рецепта: {}", recipe.getId(), e);
                }
            }

            recommendations.sort((a, b) ->
                    Double.compare(b.getMatchPercentage(), a.getMatchPercentage()));

            return recommendations.stream().limit(limit).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка в getRecommendedRecipes", e);
            return new ArrayList<>();
        }
    }

    public List<RecipeDTO> searchRecipes(String query, List<Long> productIds, int minIngredients) {
        try {
            List<Recipe> recipes;

            if (query != null && !query.isEmpty()) {
                recipes = recipeRepository.findByTitleContainingIgnoreCase(query);
            } else if (productIds != null && !productIds.isEmpty()) {
                recipes = recipeRepository.findRecipesByProducts(productIds, minIngredients);
            } else {
                recipes = recipeRepository.findByIsApprovedTrue();
            }

            return recipes.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка в searchRecipes", e);
            return new ArrayList<>();
        }
    }

    public RecipeDTO getRecipeById(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Рецепт не найден с id: " + recipeId));
        return convertToDTO(recipe);
    }

    @Transactional
    public void addToFavorites(Long userId, Long recipeId) {
        log.info("Добавление в избранное. UserId: {}, RecipeId: {}", userId, recipeId);

        if (!userFavoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            UserFavorite favorite = new UserFavorite();
            favorite.setUser(User.builder().id(userId).build());
            favorite.setRecipe(Recipe.builder().id(recipeId).build());
            userFavoriteRepository.save(favorite);
            log.info("Рецепт добавлен в избранное");
        }
    }

    @Transactional
    public void removeFromFavorites(Long userId, Long recipeId) {
        log.info("Удаление из избранного. UserId: {}, RecipeId: {}", userId, recipeId);
        userFavoriteRepository.deleteByUserIdAndRecipeId(userId, recipeId);
        log.info("Рецепт удален из избранного");
    }

    public List<RecipeDTO> getUserFavorites(Long userId) {
        return userFavoriteRepository.findByUserId(userId).stream()
                .map(favorite -> convertToDTO(favorite.getRecipe()))
                .collect(Collectors.toList());
    }

    @Transactional
    public RecipeDTO createRecipe(RecipeDTO dto) {
        log.info("Создание нового рецепта: {}", dto.getTitle());

        Recipe recipe = new Recipe();
        recipe.setTitle(dto.getTitle());
        recipe.setDescription(dto.getDescription());
        recipe.setCookingSteps(dto.getCookingSteps());
        recipe.setCookingTimeMinutes(dto.getCookingTimeMinutes());
        recipe.setDifficulty(dto.getDifficulty());
        recipe.setServings(dto.getServings());
        recipe.setCategory(dto.getCategory());
        recipe.setImageUrl(dto.getImageUrl());
        recipe.setIsApproved(true);

        Recipe savedRecipe = recipeRepository.save(recipe);
        log.info("Рецепт создан с id: {}", savedRecipe.getId());

        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {
            for (RecipeDTO.IngredientDTO ingrDto : dto.getIngredients()) {
                Product product = productRepository.findById(ingrDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Продукт не найден с id: " + ingrDto.getProductId()));

                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setRecipe(savedRecipe);
                ingredient.setProduct(product);
                ingredient.setQuantity(ingrDto.getQuantity());
                ingredient.setUnit(ingrDto.getUnit());

                recipeIngredientRepository.save(ingredient);
                log.info("Добавлен ингредиент: {}", product.getName());
            }
        }

        return convertToDTO(savedRecipe);
    }

    @Transactional
    public RecipeDTO updateRecipe(Long id, RecipeDTO dto) {
        log.info("Обновление рецепта с id: {}", id);

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Рецепт не найден с id: " + id));

        recipe.setTitle(dto.getTitle());
        recipe.setDescription(dto.getDescription());
        recipe.setCookingSteps(dto.getCookingSteps());
        recipe.setCookingTimeMinutes(dto.getCookingTimeMinutes());
        recipe.setDifficulty(dto.getDifficulty());
        recipe.setServings(dto.getServings());
        recipe.setCategory(dto.getCategory());
        recipe.setImageUrl(dto.getImageUrl());

        Recipe updatedRecipe = recipeRepository.save(recipe);
        log.info("Рецепт обновлен");

        recipeIngredientRepository.deleteByRecipeId(id);

        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {
            for (RecipeDTO.IngredientDTO ingrDto : dto.getIngredients()) {
                Product product = productRepository.findById(ingrDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Продукт не найден с id: " + ingrDto.getProductId()));

                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setRecipe(updatedRecipe);
                ingredient.setProduct(product);
                ingredient.setQuantity(ingrDto.getQuantity());
                ingredient.setUnit(ingrDto.getUnit());

                recipeIngredientRepository.save(ingredient);
            }
            log.info("Ингредиенты обновлены");
        }

        return convertToDTO(updatedRecipe);
    }

    @Transactional
    public void deleteRecipe(Long id) {
        log.info("Удаление рецепта с id: {}", id);

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Рецепт не найден с id: " + id));

        userFavoriteRepository.deleteByRecipeId(id);
        recipeIngredientRepository.deleteByRecipeId(id);
        recipeRepository.delete(recipe);

        log.info("Рецепт удален: {}", recipe.getTitle());
    }

    public List<RecipeDTO> getAllRecipesForAdmin() {
        return recipeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private RecipeDTO convertToDTO(Recipe recipe) {
        RecipeDTO dto = new RecipeDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setDescription(recipe.getDescription());
        dto.setCookingSteps(recipe.getCookingSteps());
        dto.setCookingTimeMinutes(recipe.getCookingTimeMinutes());
        dto.setDifficulty(recipe.getDifficulty());
        dto.setServings(recipe.getServings());
        dto.setCategory(recipe.getCategory());
        dto.setImageUrl(recipe.getImageUrl());
        dto.setMatchPercentage(0.0);
        dto.setIsFavorite(false);

        List<RecipeDTO.IngredientDTO> ingredientDTOs =
                recipeIngredientRepository.findByRecipeId(recipe.getId())
                        .stream()
                        .map(this::convertIngredientToDTO)
                        .collect(Collectors.toList());
        dto.setIngredients(ingredientDTOs);

        return dto;
    }

    private RecipeDTO.IngredientDTO convertIngredientToDTO(RecipeIngredient ingredient) {
        RecipeDTO.IngredientDTO dto = new RecipeDTO.IngredientDTO();
        dto.setProductId(ingredient.getProduct().getId());
        dto.setProductName(ingredient.getProduct().getName());
        dto.setQuantity(ingredient.getQuantity());
        dto.setUnit(ingredient.getUnit());
        return dto;
    }
}