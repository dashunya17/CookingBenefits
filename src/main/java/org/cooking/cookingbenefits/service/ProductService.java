package org.cooking.cookingbenefits.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cooking.cookingbenefits.dto.ProductDTO;
import org.cooking.cookingbenefits.entity.Product;
import org.cooking.cookingbenefits.entity.User;
import org.cooking.cookingbenefits.entity.UserProduct;
import org.cooking.cookingbenefits.repository.ProductRepository;
import org.cooking.cookingbenefits.repository.UserProductRepository;
import org.cooking.cookingbenefits.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserProductRepository userProductRepository;
    private final UserRepository userRepository;

    // ✅ ПОЛУЧЕНИЕ ПРОДУКТОВ ПОЛЬЗОВАТЕЛЯ
    public List<ProductDTO> getUserProducts(Long userId) {
        return userProductRepository.findByUserId(userId).stream()
                .map(this::convertToUserProductDTO)
                .collect(Collectors.toList());
    }

    // ✅ ДОБАВЛЕНИЕ ПРОДУКТА ПОЛЬЗОВАТЕЛЮ (ИСПРАВЛЕНО!)
    @Transactional
    public synchronized void addUserProduct(Long userId, ProductDTO productDTO) {
        log.info("Добавление продукта пользователю. UserId: {}, ProductId: {}", userId, productDTO.getId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Product product = productRepository.findById(productDTO.getId())
                .orElseThrow(() -> new RuntimeException("Продукт не найден с id: " + productDTO.getId()));

        if (!userProductRepository.existsByUserIdAndProductId(userId, product.getId())) {
            UserProduct userProduct = new UserProduct();
            userProduct.setUser(user);
            userProduct.setProduct(product);
            userProductRepository.save(userProduct);
            log.info("Продукт успешно добавлен");
        } else {
            log.warn("Продукт уже есть у пользователя");
        }
    }

    // ✅ УДАЛЕНИЕ ПРОДУКТА У ПОЛЬЗОВАТЕЛЯ
    @Transactional
    public synchronized void removeUserProduct(Long userId, Long productId) {
        userProductRepository.deleteByUserIdAndProductId(userId, productId);
    }

    // ✅ КАТАЛОГ ПРОДУКТОВ - ГЛАВНОЕ ИСПРАВЛЕНИЕ!
    public List<ProductDTO> getProductCatalog(String category, String search) {
        List<Product> products;

        if (category != null && !category.isEmpty() && !category.equals("null")) {
            products = productRepository.findByCategoryContainingIgnoreCase(category);
        } else if (search != null && !search.isEmpty() && !search.equals("null")) {
            products = productRepository.findByNameContainingIgnoreCase(search);
        } else {
            // ✅ ВАЖНО: ПОКАЗЫВАЕМ ВСЕ ПРОДУКТЫ!
            products = productRepository.findAll();
        }

        return products.stream()
                .map(this::convertToProductDTO)
                .collect(Collectors.toList());
    }

    // ✅ СОЗДАНИЕ ПРОДУКТА (ДЛЯ АДМИНА)
    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        if (productRepository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Продукт с таким именем уже существует");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setIsCommon(dto.getIsCommon() != null ? dto.getIsCommon() : true);

        Product saved = productRepository.save(product);
        return convertToProductDTO(saved);
    }

    // ✅ КОНВЕРТЕРЫ
    private ProductDTO convertToUserProductDTO(UserProduct userProduct) {
        ProductDTO dto = new ProductDTO();
        dto.setId(userProduct.getProduct().getId());
        dto.setName(userProduct.getProduct().getName());
        dto.setCategory(userProduct.getProduct().getCategory());
        dto.setIsCommon(userProduct.getProduct().getIsCommon());
        dto.setAddedAt(userProduct.getAddedAt());
        return dto;
    }

    private ProductDTO convertToProductDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategory(product.getCategory());
        dto.setIsCommon(product.getIsCommon());
        return dto;
    }

    // ✅ ДЛЯ АДМИНА - ВСЕ ПРОДУКТЫ
    public List<ProductDTO> getAllProductsForAdmin() {
        return productRepository.findAll().stream()
                .map(this::convertToProductDTO)
                .collect(Collectors.toList());
    }
}