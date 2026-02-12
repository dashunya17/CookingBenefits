package org.cooking.cookingbenefits.service;

import lombok.RequiredArgsConstructor;
import org.cooking.cookingbenefits.dto.ProductDTO;
import org.cooking.cookingbenefits.entity.Product;
import org.cooking.cookingbenefits.entity.User;
import org.cooking.cookingbenefits.entity.UserExcludedProduct;
import org.cooking.cookingbenefits.entity.UserProduct;
import org.cooking.cookingbenefits.repository.ProductRepository;
import org.cooking.cookingbenefits.repository.UserExcludedProductRepository;
import org.cooking.cookingbenefits.repository.UserProductRepository;
import org.cooking.cookingbenefits.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserProductRepository userProductRepository;
    private final UserExcludedProductRepository userExcludedProductRepository;
    private final UserRepository userRepository;

    public List<ProductDTO> getUserProducts(Long userId) {
        return userProductRepository.findByUserId(userId).stream()
                .map(this::convertToUserProductDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public synchronized void addUserProduct(Long userId, ProductDTO productDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Product product;

        // Если пришел существующий продукт (с ID)
        if (productDTO.getId() != null) {
            product = productRepository.findById(productDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Продукт не найден"));
        } else {
            // Если это новый продукт
            product = productRepository.findByName(productDTO.getName())
                    .orElseGet(() -> {
                        Product newProduct = new Product();
                        newProduct.setName(productDTO.getName());
                        newProduct.setCategory(productDTO.getCategory());
                        newProduct.setIsCommon(false);
                        return productRepository.save(newProduct);
                    });
        }

        // Проверяем, не добавлен ли уже
        if (!userProductRepository.existsByUserIdAndProductId(userId, product.getId())) {
            UserProduct userProduct = new UserProduct();
            userProduct.setUser(user);
            userProduct.setProduct(product);
            userProductRepository.save(userProduct);
        }
    }

    @Transactional
    public synchronized void removeUserProduct(Long userId, Long productId) {
        userProductRepository.deleteByUserIdAndProductId(userId, productId);
    }

    public List<ProductDTO> getProductCatalog(String category, String search) {
        List<Product> products;
        if (category != null && !category.isEmpty()) {
            products = productRepository.findByCategoryContainingIgnoreCase(category);
        } else if (search != null && !search.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(search);
        } else {
            products = productRepository.findCommonProducts();
        }

        return products.stream()
                .map(this::convertToProductDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addExclusion(Long userId, Long productId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Продукт не найден"));

        if (!userExcludedProductRepository.existsByUserIdAndProductId(userId, productId)) {
            UserExcludedProduct exclusion = new UserExcludedProduct();
            exclusion.setUser(user);
            exclusion.setProduct(product);
            userExcludedProductRepository.save(exclusion);
        }
    }

    // DTO для UserProduct (с addedAt)
    private ProductDTO convertToUserProductDTO(UserProduct userProduct) {
        ProductDTO dto = new ProductDTO();
        dto.setId(userProduct.getProduct().getId());
        dto.setName(userProduct.getProduct().getName());
        dto.setCategory(userProduct.getProduct().getCategory());
        dto.setIsCommon(userProduct.getProduct().getIsCommon());
        dto.setAddedAt(userProduct.getAddedAt());
        return dto;
    }

    // DTO для Product (без addedAt)
    private ProductDTO convertToProductDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategory(product.getCategory());
        dto.setIsCommon(product.getIsCommon());
        return dto;
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        // Проверка на дубликат имени
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

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Продукт не найден с id: " + id));

        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setIsCommon(dto.getIsCommon());

        return convertToProductDTO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Продукт не найден с id: " + id));

        // Проверяем, используется ли продукт
        if (!userProductRepository.findByUserId(id).isEmpty()) {
            throw new RuntimeException("Нельзя удалить продукт, который используется пользователями");
        }

        productRepository.delete(product);
    }

    public List<ProductDTO> getAllProductsForAdmin() {
        return productRepository.findAll().stream()
                .map(this::convertToProductDTO)
                .collect(Collectors.toList());
    }
}