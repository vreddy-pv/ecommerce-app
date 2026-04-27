package com.ecommerce.catalog.dto;

import java.io.Serializable;

public record CategoryDto(
    Long id,
    String name,
    Long parentId,
    String parentName
) implements Serializable {}
