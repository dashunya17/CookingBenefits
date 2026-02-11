package org.cooking.cookingbenefits.exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;

@NoArgsConstructor
@AllArgsConstructor
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}