package com.ecommerce.catalog.service;

import com.ecommerce.catalog.domain.Category;
import com.ecommerce.catalog.dto.CategoryDto;
import com.ecommerce.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private CategoryService categoryService;

    @Test
    void listAllCategoriesReturnsFlattened() {
        Category electronics = Category.builder().id(1L).name("Electronics").build();
        Category laptops = Category.builder().id(2L).name("Laptops").parent(electronics).build();
        when(categoryRepository.findAll()).thenReturn(List.of(electronics, laptops));

        List<CategoryDto> result = categoryService.listAllCategories();

        assertThat(result).hasSize(2);
    }

    @Test
    void listRootCategoriesExcludesChildren() {
        Category electronics = Category.builder().id(1L).name("Electronics").build();
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(electronics));

        List<CategoryDto> result = categoryService.listRootCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Electronics");
        assertThat(result.get(0).parentId()).isNull();
    }

    @Test
    void listAllCategoriesReturnsEmptyListWhenNoneExist() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryDto> result = categoryService.listAllCategories();

        assertThat(result).isEmpty();
    }
}
