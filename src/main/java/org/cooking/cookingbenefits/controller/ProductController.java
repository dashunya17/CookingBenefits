package org.cooking.cookingbenefits.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cooking.cookingbenefits.dto.ProductDTO;
import org.cooking.cookingbenefits.entity.User;
import org.cooking.cookingbenefits.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/available")
    public ResponseEntity<List<ProductDTO>> getUserProducts(@AuthenticationPrincipal User user) {
        List<ProductDTO> products = productService.getUserProducts(user.getId());
        return ResponseEntity.ok(products);
    }

    @PostMapping("/available")
    public ResponseEntity<Void> addUserProduct(
            @AuthenticationPrincipal User user,
            @RequestBody ProductDTO productDTO) {

        productService.addUserProduct(user.getId(), productDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/available/{productId}")
    public ResponseEntity<Void> removeUserProduct(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId) {

        productService.removeUserProduct(user.getId(), productId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/catalog")
    public ResponseEntity<List<ProductDTO>> getProductCatalog(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "") String search) {

        List<ProductDTO> products = productService.getProductCatalog(category, search);
        return ResponseEntity.ok(products);
    }

    @PostMapping("/exclusions")
    public ResponseEntity<Void> addExclusion(
            @AuthenticationPrincipal User user,
            @RequestParam Long productId,
            @RequestParam(required = false) String reason) {

        productService.addExclusion(user.getId(), productId, reason);
        return ResponseEntity.ok().build();
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO dto) {
        ProductDTO created = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        ProductDTO updated = productService.updateProduct(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductDTO>> getAllProductsForAdmin() {
        List<ProductDTO> products = productService.getAllProductsForAdmin();
        return ResponseEntity.ok(products);
    }
}