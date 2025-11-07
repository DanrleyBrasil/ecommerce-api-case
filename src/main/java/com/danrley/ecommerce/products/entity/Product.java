package com.danrley.ecommerce.products.entity;

import com.danrley.ecommerce.shared.converter.JsonConverter;
import com.danrley.ecommerce.shared.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa um produto no catálogo do e-commerce.
 * Usa JSON para armazenar specs técnicas flexíveis (marca, modelo, etc).
 * Controla estoque real e reservado para evitar overselling (ADR-003).
 */
@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Estoque físico real do produto.
     */
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    /**
     * Estoque temporariamente reservado durante checkout (TTL 10min).
     * Usado na estratégia híbrida de controle de estoque (ADR-003).
     */
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Fornecedor é opcional - produtos podem não ter fornecedor cadastrado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(unique = true, length = 50)
    private String sku;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Specs técnicas flexíveis armazenadas como JSON.
     * Exemplo: {"brand": "Logitech", "dpi": 8000}
     */
    @Convert(converter = JsonConverter.class)  // ✅ USA ISSO
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Calcula quanto estoque está disponível para venda.
     * Considera estoque real menos o que já está reservado.
     *
     * @return quantidade disponível para novas reservas
     */
    public Integer getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    /**
     * Verifica se há estoque suficiente disponível.
     *
     * @param quantity quantidade desejada
     * @return true se há estoque disponível
     */
    public boolean hasStock(Integer quantity) {
        return getAvailableQuantity() >= quantity;
    }
}