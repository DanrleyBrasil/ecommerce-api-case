package com.danrley.ecommerce.products.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO para criação e atualização de produtos.
 * Usado nos endpoints POST e PUT.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    private String name;

    @Size(max = 5000, message = "Descrição deve ter no máximo 5000 caracteres")
    private String description;

    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Preço inválido")
    private BigDecimal price;

    @NotNull(message = "Quantidade em estoque é obrigatória")
    @Min(value = 0, message = "Estoque não pode ser negativo")
    private Integer stockQuantity;

    @NotNull(message = "Categoria é obrigatória")
    private Long categoryId;

    /**
     * Fornecedor é OPCIONAL
     */
    private Long supplierId;

    @Size(max = 50, message = "SKU deve ter no máximo 50 caracteres")
    private String sku;

    /**
     * Metadados flexíveis do produto (JSON).
     * Exemplo: {"brand": "Logitech", "color": "Preto", "warranty": "12 meses"}
     */
    private Map<String, Object> metadata;

    private Boolean active = true;
}