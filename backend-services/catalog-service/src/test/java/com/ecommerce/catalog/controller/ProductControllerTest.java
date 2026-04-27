package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.dto.ProductDto;
import com.ecommerce.catalog.service.ProductService;
import com.ecommerce.common.dto.PagedResponse;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({com.ecommerce.catalog.config.SecurityConfig.class,
         com.ecommerce.common.exception.GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private ProductService productService;

    private final ProductDto sampleProduct = new ProductDto(
        1L, "LAP-001", "Pro Laptop", "16GB RAM",
        new BigDecimal("1299.99"), 1L, "Electronics", List.of(), true);

    @Test
    void getProductsReturns200WithPaginatedList() throws Exception {
        PagedResponse<ProductDto> page = PagedResponse.<ProductDto>builder()
            .content(List.of(sampleProduct)).page(0).size(10)
            .totalElements(1).totalPages(1).last(true).build();

        when(productService.listProducts(any())).thenReturn(page);

        mockMvc.perform(get("/api/catalog/products")
                .param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].name").value("Pro Laptop"))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getProductByIdReturns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleProduct);

        mockMvc.perform(get("/api/catalog/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sku").value("LAP-001"))
            .andExpect(jsonPath("$.data.price").value(1299.99));
    }

    @Test
    void getProductByIdReturns404WhenNotFound() throws Exception {
        when(productService.getProductById(99L))
            .thenThrow(new ResourceNotFoundException("Product", 99L));

        mockMvc.perform(get("/api/catalog/products/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void searchProductsReturns200() throws Exception {
        PagedResponse<ProductDto> page = PagedResponse.<ProductDto>builder()
            .content(List.of(sampleProduct)).page(0).size(10)
            .totalElements(1).totalPages(1).last(true).build();

        when(productService.searchProducts(eq("laptop"), any())).thenReturn(page);

        mockMvc.perform(get("/api/catalog/products/search")
                .param("q", "laptop"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content[0].name").value("Pro Laptop"));
    }

    @Test
    void createProductRequiresAdminRole() throws Exception {
        // Request without ADMIN header → 403
        mockMvc.perform(post("/api/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sku":"LAP-002","name":"New Laptop","price":999.99,"categoryId":1}
                    """)
                .header("X-User-Role", "USER"))
            .andExpect(status().isForbidden());
    }

    @Test
    void createProductSucceedsWithAdminRole() throws Exception {
        when(productService.createProduct(any())).thenReturn(sampleProduct);

        mockMvc.perform(post("/api/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sku":"LAP-001","name":"Pro Laptop","description":"16GB RAM",
                     "price":1299.99,"categoryId":1}
                    """)
                .header("X-User-Role", "ADMIN"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.sku").value("LAP-001"));
    }

    @Test
    void createProductReturns400WhenSkuMissing() throws Exception {
        mockMvc.perform(post("/api/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"No SKU Laptop","price":999.99,"categoryId":1}
                    """)
                .header("X-User-Role", "ADMIN"))
            .andExpect(status().isBadRequest());
    }
}
