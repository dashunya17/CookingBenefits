package org.cooking.cookingbenefits.controller;

import lombok.RequiredArgsConstructor;
import org.cooking.cookingbenefits.dto.ProductDTO;
import org.cooking.cookingbenefits.entity.User;
import org.cooking.cookingbenefits.service.ProductService;
import org.springframework.http.ResponseEntity;
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
}