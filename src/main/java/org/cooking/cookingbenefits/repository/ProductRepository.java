package org.cooking.cookingbenefits.repository;

import org.cooking.cookingbenefits.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByCategoryContainingIgnoreCase(String category);

    @Query("SELECT p FROM Product p WHERE p.isCommon = true")
    List<Product> findCommonProducts();
}

