package org.cooking.cookingbenefits.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String category;
    private Boolean isCommon; // Добавьте это поле
    private LocalDateTime addedAt;

}