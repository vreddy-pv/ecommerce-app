package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.dto.CategoryDto;
import com.ecommerce.catalog.service.CategoryService;
import com.ecommerce.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryDto>> listCategories(
            @RequestParam(defaultValue = "false") boolean rootOnly) {
        var result = rootOnly
            ? categoryService.listRootCategories()
            : categoryService.listAllCategories();
        return ApiResponse.ok(result);
    }
}
