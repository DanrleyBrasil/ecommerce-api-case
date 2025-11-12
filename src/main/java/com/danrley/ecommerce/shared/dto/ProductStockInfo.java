package com.danrley.ecommerce.shared.dto;

import java.math.BigDecimal;

public record ProductStockInfo(
        Long id,
        String name,
        BigDecimal price,
        int stockQuantity,
        int reservedQuantity,
        boolean active
) {}