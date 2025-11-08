package com.danrley.ecommerce.products.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de resposta para produtos.
 * Usado nos endpoints GET.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Integer reservedQuantity;

    /**
     * Estoque disponível para venda (stockQuantity - reservedQuantity)
     */
    private Integer availableQuantity;

    private CategoryResponse category;
    private SupplierResponse supplier; // PODE SER NULL
    private String sku;
    private Boolean active;
    private Map<String, Object> metadata;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}