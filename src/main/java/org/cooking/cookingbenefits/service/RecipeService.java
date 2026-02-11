package org.cooking.cookingbenefits.service;

import lombok.RequiredArgsConstructor;
import org.cooking.cookingbenefits.dto.RecipeDTO;
import org.cooking.cookingbenefits.entity.*;
import org.cooking.cookingbenefits.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserProductRepository userProductRepository;
    private final UserExcludedProductRepository userExclusionRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final UserFavoriteRepository userFavoriteRepository;

    public List<RecipeDTO> getRecommendedRecipes(Long userId, int limit) {
        // 1. Получить доступные продукты пользователя
        List<UserProduct> userProducts = userProductRepository.findByUserId(userId);
        Set<Long> availableProductIds = userProducts.stream()
                .map(up -> up.getProduct().getId())
                .collect(Collectors.toSet());

        // 2. Получить исключенные продукты
        Set<Long> excludedProductIds = userExclusionRepository.findByUserId(userId)
                .stream()
                .map(ue -> ue.getProduct().getId())
                .collect(Collectors.toSet());

        // 3. Получить все рецепты
        List<Recipe> allRecipes = recipeRepository.findApprovedRecipes();

        // 4. Рассчитать рейтинг для каждого рецепта
        Map<Recipe, RecipeScore> recipeScores = new HashMap<>();

        for (Recipe recipe : allRecipes) {
            RecipeScore score = calculateRecipeScore(recipe, availableProductIds, excludedProductIds);
            if (score.getMatchPercentage() > 0) {
                recipeScores.put(recipe, score);
            }
        }

        // 5. Сортировка по рейтингу
        return recipeScores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().getScore(), e1.getValue().getScore()))
                .limit(limit)
                .map(entry -> {
                    Recipe recipe = entry.getKey();
                    RecipeScore score = entry.getValue();
                    RecipeDTO dto = convertToDTO(recipe);
                    dto.setMatchPercentage(score.getMatchPercentage());
                    dto.setMissingIngredients(score.getMissingIngredients());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public RecipeDTO getRecipeById(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Рецепт не найден"));

        return convertToDTO(recipe);
    }

    @Transactional
    public void addToFavorites(Long userId, Long recipeId) {
        // Проверяем, не добавлен ли уже в избранное
        if (!userFavoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            UserFavorite favorite = new UserFavorite();
            favorite.setUser(User.builder().id(userId).build());
            favorite.setRecipe(Recipe.builder().id(recipeId).build());
            userFavoriteRepository.save(favorite);
        }
    }
    public List<RecipeDTO> searchRecipes(String query, List<Long> productIds, int minIngredients) {
        List<Recipe> recipes;

        if (query != null && !query.isEmpty()) {
            // Поиск по названию
            recipes = recipeRepository.findByTitleContainingIgnoreCase(query);
        } else if (productIds != null && !productIds.isEmpty()) {
            // Поиск по продуктам
            recipes = recipeRepository.findRecipesByProducts(productIds, minIngredients);
        } else {
            // Все рецепты
            recipes = recipeRepository.findApprovedRecipes();
        }

        return recipes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public void removeFromFavorites(Long userId, Long recipeId) {
        userFavoriteRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }

    public List<RecipeDTO> getUserFavorites(Long userId) {
        return userFavoriteRepository.findByUserId(userId).stream()
                .map(favorite -> convertToDTO(favorite.getRecipe()))
                .collect(Collectors.toList());
    }

    private RecipeScore calculateRecipeScore(Recipe recipe,
                                             Set<Long> availableProductIds,
                                             Set<Long> excludedProductIds) {
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByRecipeId(recipe.getId());

        int totalIngredients = ingredients.size();
        int availableCount = 0;
        int excludedCount = 0;
        List<String> missingIngredients = new ArrayList<>();

        for (RecipeIngredient ingredient : ingredients) {
            Long productId = ingredient.getProduct().getId();

            // Проверка на исключения
            if (excludedProductIds.contains(productId)) {
                excludedCount++;
                continue;
            }

            // Проверка на наличие
            if (availableProductIds.contains(productId)) {
                availableCount++;
            } else {
                missingIngredients.add(ingredient.getProduct().getName());
            }
        }

        // Расчет процента совпадения
        double matchPercentage = 0;
        if (totalIngredients > 0) {
            matchPercentage = (double) availableCount / totalIngredients * 100;
        }

        // Штраф за исключенные ингредиенты
        if (excludedCount > 0) {
            matchPercentage *= 0.5;
        }

        // Бонус за полное совпадение
        double score = matchPercentage;
        if (missingIngredients.isEmpty() && excludedCount == 0) {
            score += 30;
        }

        // Учет сложности рецепта
        if ("легко".equalsIgnoreCase(recipe.getDifficulty())) {
            score += 5;
        }

        return new RecipeScore(score, matchPercentage, missingIngredients);
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

        // Конвертация ингредиентов
        List<RecipeDTO.IngredientDTO> ingredientDTOs = recipeIngredientRepository.findByRecipeId(recipe.getId())
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

    // Вспомогательный класс
    private static class RecipeScore {
        private final double score;
        private final double matchPercentage;
        private final List<String> missingIngredients;

        public RecipeScore(double score, double matchPercentage, List<String> missingIngredients) {
            this.score = score;
            this.matchPercentage = matchPercentage;
            this.missingIngredients = missingIngredients;
        }

        public double getScore() { return score; }
        public double getMatchPercentage() { return matchPercentage; }
        public List<String> getMissingIngredients() { return missingIngredients; }
    }
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
        recipe.setIsApproved(true); // или берите из dto, если нужно

        Recipe savedRecipe = recipeRepository.save(recipe);

        if (dto.getIngredients() != null) {
            for (RecipeDTO.IngredientDTO ingrDto : dto.getIngredients()) {
                Product product = productRepository.findById(ingrDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found with id: " + ingrDto.getProductId()));

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

    @Transactional
    public RecipeDTO updateRecipe(Long id, RecipeDTO dto) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + id));

        recipe.setTitle(dto.getTitle());
        recipe.setDescription(dto.getDescription());
        recipe.setCookingSteps(dto.getCookingSteps());
        recipe.setCookingTimeMinutes(dto.getCookingTimeMinutes());
        recipe.setDifficulty(dto.getDifficulty());
        recipe.setServings(dto.getServings());
        recipe.setCategory(dto.getCategory());
        recipe.setImageUrl(dto.getImageUrl());

        // Удаляем старые ингредиенты
        recipeIngredientRepository.deleteByRecipeId(id);

        // Добавляем новые
        if (dto.getIngredients() != null) {
            for (RecipeDTO.IngredientDTO ingrDto : dto.getIngredients()) {
                Product product = productRepository.findById(ingrDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found with id: " + ingrDto.getProductId()));

                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setRecipe(recipe);
                ingredient.setProduct(product);
                ingredient.setQuantity(ingrDto.getQuantity());
                ingredient.setUnit(ingrDto.getUnit());

                recipeIngredientRepository.save(ingredient);
            }
        }

        return convertToDTO(recipe);
    }

    @Transactional
    public void deleteRecipe(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found with id: " + id));

        // Удаляем связанные записи
        userFavoriteRepository.deleteByRecipeId(id);
        recipeIngredientRepository.deleteByRecipeId(id);
        recipeRepository.delete(recipe);
    }

    public List<RecipeDTO> getAllRecipesForAdmin() {
        return recipeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

}