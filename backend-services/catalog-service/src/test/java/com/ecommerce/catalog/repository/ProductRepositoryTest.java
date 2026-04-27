package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.domain.Category;
import com.ecommerce.catalog.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;

    private Category electronics;

    @BeforeEach
    void setUp() {
        electronics = categoryRepository.save(
            Category.builder().name("Electronics").build());
    }

    @Test
    void findByIsActiveTrueReturnsOnlyActiveProducts() {
        productRepository.save(activeProduct("LAP-001", "Active Laptop"));
        productRepository.save(inactiveProduct("LAP-002", "Deleted Laptop"));

        Page<Product> result = productRepository.findByIsActiveTrue(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSku()).isEqualTo("LAP-001");
    }

    @Test
    void findByIdAndIsActiveTrueReturnsEmptyForInactiveProduct() {
        Product inactive = productRepository.save(inactiveProduct("LAP-DEL", "Deleted"));

        var result = productRepository.findByIdAndIsActiveTrue(inactive.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByCategoryIdAndIsActiveTrueFiltersCorrectly() {
        productRepository.save(activeProduct("LAP-001", "Laptop"));
        Category other = categoryRepository.save(Category.builder().name("Phones").build());
        productRepository.save(Product.builder()
            .sku("PHN-001").name("Phone").price(new BigDecimal("799.99"))
            .category(other).isActive(true).build());

        Page<Product> result = productRepository
            .findByCategory_IdAndIsActiveTrue(electronics.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSku()).isEqualTo("LAP-001");
    }

    @Test
    void searchByKeywordMatchesNameCaseInsensitive() {
        productRepository.save(activeProduct("LAP-001", "Pro Laptop"));
        productRepository.save(activeProduct("PHN-001", "Smart Phone"));

        Page<Product> result = productRepository
            .searchByKeyword("laptop", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Pro Laptop");
    }

    @Test
    void searchByKeywordMatchesDescription() {
        Product p = activeProduct("LAP-001", "Pro Laptop");
        p.setDescription("Powerful 16GB RAM machine");
        productRepository.save(p);

        Page<Product> result = productRepository
            .searchByKeyword("16GB", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void paginationReturnsCorrectSlice() {
        for (int i = 1; i <= 15; i++) {
            productRepository.save(activeProduct("SKU-" + i, "Product " + i));
        }

        Page<Product> page0 = productRepository.findByIsActiveTrue(PageRequest.of(0, 10));
        Page<Product> page1 = productRepository.findByIsActiveTrue(PageRequest.of(1, 10));

        assertThat(page0.getContent()).hasSize(10);
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page0.getTotalElements()).isEqualTo(15);
    }

    private Product activeProduct(String sku, String name) {
        return Product.builder().sku(sku).name(name)
            .price(new BigDecimal("999.99")).category(electronics).isActive(true).build();
    }

    private Product inactiveProduct(String sku, String name) {
        return Product.builder().sku(sku).name(name)
            .price(new BigDecimal("999.99")).category(electronics).isActive(false).build();
    }
}
