package com.danrley.ecommerce.products.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para filtros de busca de produtos.
 * Todos os campos são OPCIONAIS.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {

    /**
     * Busca por nome (contém - case insensitive)
     */
    private String name;

    /**
     * Filtro por categoria
     */
    private Long categoryId;

    /**
     * Filtro por fornecedor
     */
    private Long supplierId;

    /**
     * Preço mínimo
     */
    private BigDecimal minPrice;

    /**
     * Preço máximo
     */
    private BigDecimal maxPrice;

    /**
     * Filtrar apenas ativos ou inativos
     */
    private Boolean active;
}