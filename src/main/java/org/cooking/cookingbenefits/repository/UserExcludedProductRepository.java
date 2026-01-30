package org.cooking.cookingbenefits.repository;

import org.cooking.cookingbenefits.entity.UserExcludedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserExcludedProductRepository extends JpaRepository<UserExcludedProduct, Long> {
    List<UserExcludedProduct> findByUserId(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
