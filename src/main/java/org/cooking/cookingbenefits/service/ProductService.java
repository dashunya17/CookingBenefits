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
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addUserProduct(Long userId, ProductDTO productDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Product product = productRepository.findById(productDTO.getId())
                .orElseGet(() -> {
                    // Создаем новый продукт если не существует
                    Product newProduct = new Product();
                    newProduct.setName(productDTO.getName());
                    newProduct.setCategory(productDTO.getCategory());
                    newProduct.setIsCommon(false); // Пользовательский продукт
                    return productRepository.save(newProduct);
                });

        // Проверяем, не добавлен ли уже
        if (!userProductRepository.existsByUserIdAndProductId(userId, product.getId())) {
            UserProduct userProduct = new UserProduct();
            userProduct.setUser(user);
            userProduct.setProduct(product);
            userProductRepository.save(userProduct);
        }
    }

    @Transactional
    public void removeUserProduct(Long userId, Long productId) {
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
                .map(this::convertToDTO)
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

    private ProductDTO convertToDTO(UserProduct userProduct) {
        ProductDTO dto = new ProductDTO();
        dto.setId(userProduct.getProduct().getId());
        dto.setName(userProduct.getProduct().getName());
        dto.setCategory(userProduct.getProduct().getCategory());
        dto.setIsCommon(userProduct.getProduct().getIsCommon());
        dto.setAddedAt(userProduct.getAddedAt());
        return dto;
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        // Проверка на дубликат имени (опционально)
        if (productRepository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Продукт с таким именем уже существует");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setIsCommon(dto.getIsCommon() != null ? dto.getIsCommon() : true);
        // addedAt установится автоматически через @CreationTimestamp

        Product saved = productRepository.save(product);
        return convertToDTO(saved);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Продукт не найден с id: " + id));

        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setIsCommon(dto.getIsCommon());

        return convertToDTO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Продукт не найден с id: " + id));

        // Важно: перед удалением нужно убедиться, что продукт не используется
        // В вашей модели есть связи: UserProduct, RecipeIngredient, UserExcludedProduct
        // Если не настроено каскадное удаление, нужно либо запретить удаление,
        // либо удалять связи вручную. Для простоты — разрешим удаление,
        // но вы можете добавить проверку:

        productRepository.delete(product);
    }

    // Вспомогательный метод конвертации (уже есть, но убедитесь, что он существует)
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategory(product.getCategory());
        dto.setIsCommon(product.getIsCommon());
        // addedAt не маппим, так как это DTO для каталога, дата добавления не нужна
        return dto;
    }
}