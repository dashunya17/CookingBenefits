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
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final ProductRepository productRepository;

    // ✅ ПОЛУЧЕНИЕ РЕКОМЕНДАЦИЙ - ИСПРАВЛЕНО!
    public List<RecipeDTO> getRecommendedRecipes(Long userId, int limit) {
        try {
            log.info("Получение рекомендаций для пользователя: {}", userId);

            List<Recipe> allRecipes = recipeRepository.findByIsApprovedTrue();

            if (allRecipes.isEmpty()) {
                log.warn("Нет одобренных рецептов в базе");
                return new ArrayList<>();
            }

            Set<Long> availableProductIds = Collections.emptySet();

            // Получаем продукты пользователя, если он авторизован
            if (userId != null) {
                availableProductIds = userProductRepository.findByUserId(userId).stream()
                        .map(up -> up.getProduct().getId())
                        .collect(Collectors.toSet());
            }

            List<RecipeDTO> recommendations = new ArrayList<>();

            for (Recipe recipe : allRecipes) {
                try {
                    RecipeDTO dto = convertToDTO(recipe);

                    // Рассчитываем процент совпадения
                    List<RecipeIngredient> ingredients = recipeIngredientRepository.findByRecipeId(recipe.getId());
                    int totalIngredients = ingredients.size();
                    int availableCount = 0;

                    if (totalIngredients > 0) {
                        for (RecipeIngredient ingredient : ingredients) {
                            if (availableProductIds.contains(ingredient.getProduct().getId())) {
                                availableCount++;
                            }
                        }
                        double matchPercentage = (double) availableCount / totalIngredients * 100;
                        dto.setMatchPercentage(Math.round(matchPercentage * 10) / 10.0);
                    } else {
                        dto.setMatchPercentage(0.0);
                    }

                    // Проверяем избранное
                    if (userId != null) {
                        dto.setIsFavorite(userFavoriteRepository.existsByUserIdAndRecipeId(userId, recipe.getId()));
                    } else {
                        dto.setIsFavorite(false);
                    }

                    recommendations.add(dto);
                } catch (Exception e) {
                    log.error("Ошибка обработки рецепта: {}", recipe.getId(), e);
                }
            }

            // Сортируем по проценту совпадения
            recommendations.sort((a, b) ->
                    Double.compare(b.getMatchPercentage(), a.getMatchPercentage()));

            return recommendations.stream().limit(limit).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка в getRecommendedRecipes", e);
            return new ArrayList<>(); // НИКОГДА НЕ ВОЗВРАЩАЕМ 500!
        }
    }

    // ✅ ПОЛУЧЕНИЕ РЕЦЕПТА ПО ID
    public RecipeDTO getRecipeById(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Рецепт не найден"));
        return convertToDTO(recipe);
    }

    // ✅ ДОБАВЛЕНИЕ В ИЗБРАННОЕ
    @Transactional
    public void addToFavorites(Long userId, Long recipeId) {
        if (!userFavoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            UserFavorite favorite = new UserFavorite();
            favorite.setUser(User.builder().id(userId).build());
            favorite.setRecipe(Recipe.builder().id(recipeId).build());
            userFavoriteRepository.save(favorite);
        }
    }

    // ✅ УДАЛЕНИЕ ИЗ ИЗБРАННОГО
    @Transactional
    public void removeFromFavorites(Long userId, Long recipeId) {
        userFavoriteRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }

    // ✅ ПОЛУЧЕНИЕ ИЗБРАННОГО
    public List<RecipeDTO> getUserFavorites(Long userId) {
        return userFavoriteRepository.findByUserId(userId).stream()
                .map(favorite -> convertToDTO(favorite.getRecipe()))
                .collect(Collectors.toList());
    }

    // ✅ КОНВЕРТЕР
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

        // Конвертация ингредиентов
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

    // ✅ АДМИН: СОЗДАНИЕ РЕЦЕПТА
    @Transactional
    public RecipeDTO createRecipe(RecipeDTO dto) {
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

        if (dto.getIngredients() != null) {
            for (RecipeDTO.IngredientDTO ingrDto : dto.getIngredients()) {
                Product product = productRepository.findById(ingrDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setRecipe(savedRecipe);
                ingredient.setProduct(product);
                ingredient.setQuantity(ingrDto.getQuantity());
                ingredient.setUnit(ingrDto.getUnit());

                recipeIngredientRepository.save(ingredient);
            }
        }

        return convertToDTO(savedRecipe);
    }

    // ✅ АДМИН: ВСЕ РЕЦЕПТЫ
    public List<RecipeDTO> getAllRecipesForAdmin() {
        return recipeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}