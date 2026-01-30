package org.cooking.cookingbenefits.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecipeDTO {
    private Long id;
    private String title;
    private String description;
    private String cookingSteps;
    private Integer cookingTimeMinutes;
    private String difficulty;
    private Integer servings;
    private String category;
    private String imageUrl;
    private Double matchPercentage;
    private List<String> missingIngredients;
    private List<IngredientDTO> ingredients;
    private Boolean isFavorite;

    @Data
    public static class IngredientDTO {
        private Long productId;
        private String productName;
        private Double quantity;
        private String unit;
    }
}