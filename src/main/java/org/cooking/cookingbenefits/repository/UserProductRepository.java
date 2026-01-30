package org.cooking.cookingbenefits.repository;

import org.cooking.cookingbenefits.entity.UserProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserProductRepository extends JpaRepository<UserProduct, Long> {
    List<UserProduct> findByUserId(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
