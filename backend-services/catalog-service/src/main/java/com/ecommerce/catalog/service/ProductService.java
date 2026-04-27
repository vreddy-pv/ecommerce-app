package com.ecommerce.catalog.service;

import com.ecommerce.catalog.domain.Product;
import com.ecommerce.catalog.domain.ProductImage;
import com.ecommerce.catalog.dto.CreateProductRequest;
import com.ecommerce.catalog.dto.ProductDto;
import com.ecommerce.catalog.repository.CategoryRepository;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.dto.PagedResponse;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ── Read operations (cached in Redis Object Cache DB 1) ─────────────────

    @Cacheable(value = "products", key = "'page-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PagedResponse<ProductDto> listProducts(Pageable pageable) {
        return toPagedResponse(productRepository.findByIsActiveTrue(pageable));
    }

    @Cacheable(value = "products", key = "'cat-' + #categoryId + '-p' + #pageable.pageNumber")
    public PagedResponse<ProductDto> listProductsByCategory(Long categoryId, Pageable pageable) {
        return toPagedResponse(
            productRepository.findByCategory_IdAndIsActiveTrue(categoryId, pageable));
    }

    @Cacheable(value = "products", key = "#id")
    public ProductDto getProductById(Long id) {
        return productRepository.findByIdAndIsActiveTrue(id)
            .map(this::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Cacheable(value = "products", key = "'search-' + #keyword + '-p' + #pageable.pageNumber",
               condition = "#keyword != null && !#keyword.isBlank()")
    public PagedResponse<ProductDto> searchProducts(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return listProducts(pageable);
        }
        return toPagedResponse(productRepository.searchByKeyword(keyword, pageable));
    }

    // ── Write operations (evict all product caches on any mutation) ──────────

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true)
    })
    public ProductDto createProduct(CreateProductRequest request) {
        var category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        Product product = Product.builder()
            .sku(request.sku())
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .category(category)
            .build();

        addImages(product, request.imageUrls());
        return toDto(productRepository.save(product));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "products", allEntries = true)
    })
    public ProductDto updateProduct(Long id, CreateProductRequest request) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        var category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(category);
        product.getImages().clear();
        addImages(product, request.imageUrls());

        return toDto(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deactivateProduct(Long id) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false);
        productRepository.save(product);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    ProductDto toDto(Product p) {
        List<String> imageUrls = p.getImages().stream()
            .sorted(java.util.Comparator.comparingInt(ProductImage::getDisplayOrder))
            .map(ProductImage::getImageUrl)
            .toList();

        return new ProductDto(
            p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(),
            p.getCategory().getId(), p.getCategory().getName(),
            imageUrls, p.isActive());
    }

    private PagedResponse<ProductDto> toPagedResponse(Page<Product> page) {
        return PagedResponse.<ProductDto>builder()
            .content(page.getContent().stream().map(this::toDto).toList())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
    }

    private void addImages(Product product, List<String> imageUrls) {
        if (imageUrls == null) return;
        for (int i = 0; i < imageUrls.size(); i++) {
            product.getImages().add(ProductImage.builder()
                .product(product).imageUrl(imageUrls.get(i)).displayOrder(i).build());
        }
    }
}
