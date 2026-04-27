package com.ecommerce.catalog.service;

import com.ecommerce.catalog.domain.Category;
import com.ecommerce.catalog.domain.Product;
import com.ecommerce.catalog.dto.CreateProductRequest;
import com.ecommerce.catalog.dto.ProductDto;
import com.ecommerce.catalog.repository.CategoryRepository;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private ProductService productService;

    private Category electronics;
    private Product laptop;

    @BeforeEach
    void setUp() {
        electronics = Category.builder().id(1L).name("Electronics").build();
        laptop = Product.builder()
            .id(1L).sku("LAP-001").name("Pro Laptop").description("16GB RAM, 512GB SSD")
            .price(new BigDecimal("1299.99")).category(electronics).isActive(true)
            .build();
    }

    // ── List & Pagination ─────────────────────────────────────────────────────

    @Test
    void listProductsReturnsPaginatedResults() {
        Page<Product> page = new PageImpl<>(List.of(laptop), PageRequest.of(0, 10), 1);
        when(productRepository.findByIsActiveTrue(any())).thenReturn(page);

        var result = productService.listProducts(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Pro Laptop");
    }

    @Test
    void listProductsByCategoryFiltersCorrectly() {
        Page<Product> page = new PageImpl<>(List.of(laptop), PageRequest.of(0, 10), 1);
        when(productRepository.findByCategory_IdAndIsActiveTrue(eq(1L), any())).thenReturn(page);

        var result = productService.listProductsByCategory(1L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).categoryId()).isEqualTo(1L);
    }

    @Test
    void listProductsReturnsEmptyPageWhenNoneExist() {
        when(productRepository.findByIsActiveTrue(any())).thenReturn(Page.empty());

        var result = productService.listProducts(PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ── Get Single Product ────────────────────────────────────────────────────

    @Test
    void getProductByIdReturnsCorrectProduct() {
        when(productRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(laptop));

        ProductDto result = productService.getProductById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sku()).isEqualTo("LAP-001");
        assertThat(result.price()).isEqualByComparingTo("1299.99");
    }

    @Test
    void getProductByIdThrowsWhenNotFound() {
        when(productRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    void searchProductsByNameReturnsMatches() {
        Page<Product> page = new PageImpl<>(List.of(laptop), PageRequest.of(0, 10), 1);
        when(productRepository.searchByKeyword(eq("laptop"), any())).thenReturn(page);

        var result = productService.searchProducts("laptop", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchWithEmptyQueryReturnsAllProducts() {
        Page<Product> page = new PageImpl<>(List.of(laptop), PageRequest.of(0, 10), 1);
        when(productRepository.findByIsActiveTrue(any())).thenReturn(page);

        var result = productService.searchProducts("", PageRequest.of(0, 10));

        verify(productRepository).findByIsActiveTrue(any());
        verify(productRepository, never()).searchByKeyword(any(), any());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void createProductSavesAndReturnsDto() {
        CreateProductRequest request = new CreateProductRequest(
            "LAP-002", "Gaming Laptop", "RTX 4080",
            new BigDecimal("1999.99"), 1L, null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(productRepository.save(any())).thenReturn(laptop);

        ProductDto result = productService.createProduct(request);

        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProductThrowsWhenCategoryNotFound() {
        CreateProductRequest request = new CreateProductRequest(
            "LAP-002", "Gaming Laptop", "RTX 4080",
            new BigDecimal("1999.99"), 99L, null);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void updateProductChangesFieldsAndSaves() {
        when(productRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(laptop));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
        when(productRepository.save(any())).thenReturn(laptop);

        CreateProductRequest update = new CreateProductRequest(
            "LAP-001", "Updated Laptop", "New description",
            new BigDecimal("999.99"), 1L, null);

        ProductDto result = productService.updateProduct(1L, update);

        verify(productRepository).save(laptop);
        assertThat(laptop.getName()).isEqualTo("Updated Laptop");
        assertThat(laptop.getPrice()).isEqualByComparingTo("999.99");
    }
}
