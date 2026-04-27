package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.dto.CreateProductRequest;
import com.ecommerce.catalog.dto.ProductDto;
import com.ecommerce.catalog.service.ProductService;
import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PagedResponse<ProductDto>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId) {

        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        var result = categoryId != null
            ? productService.listProductsByCategory(categoryId, pageable)
            : productService.listProducts(pageable);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDto> getProduct(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    public ApiResponse<PagedResponse<ProductDto>> search(
            @RequestParam(name = "q", defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size);
        return ApiResponse.ok(productService.searchProducts(keyword, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDto> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @RequestHeader("X-User-Role") String role) {

        requireAdmin(role);
        return ApiResponse.ok(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest request,
            @RequestHeader("X-User-Role") String role) {

        requireAdmin(role);
        return ApiResponse.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProduct(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {

        requireAdmin(role);
        productService.deactivateProduct(id);
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new com.ecommerce.common.exception.ServiceException(
                "Admin role required", "FORBIDDEN", org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }
}
