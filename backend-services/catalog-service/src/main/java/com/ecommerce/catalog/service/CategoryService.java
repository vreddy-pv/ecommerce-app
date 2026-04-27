package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.CategoryDto;
import com.ecommerce.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryDto> listAllCategories() {
        return categoryRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }

    @Cacheable(value = "categories", key = "'root'")
    public List<CategoryDto> listRootCategories() {
        return categoryRepository.findByParentIsNull().stream()
            .map(this::toDto)
            .toList();
    }

    private CategoryDto toDto(com.ecommerce.catalog.domain.Category c) {
        return new CategoryDto(
            c.getId(), c.getName(),
            c.getParent() != null ? c.getParent().getId() : null,
            c.getParent() != null ? c.getParent().getName() : null);
    }
}
