package com.danrley.ecommerce.orders.entity;

import com.danrley.ecommerce.products.entity.Product;
import com.danrley.ecommerce.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Entidade que representa um item individual de um pedido.
 * <p>
 * Características importantes:
 * - Armazena snapshot do preço no momento da compra (unitPrice)
 * - Relacionamento Many-to-One com Order e Product
 * - Subtotal calculado automaticamente (quantity × unitPrice)
 * - Não permite alteração após criação (imutável por regra de negócio)
 * <p>
 * Por que armazenar unitPrice?
 * - Histórico: Preço do produto pode mudar, mas pedido mantém preço da compra
 * - Exemplo: Produto custava R$ 100 na compra, depois mudou para R$ 120
 * → OrderItem mantém unitPrice = 100
 *
 * @author Danrley Brasil dos Santos
 * @see Order
 * @see Product
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true) // Adicionado para conformidade com @Data e herança
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id"),
        @Index(name = "idx_order_items_product", columnList = "product_id")
})
public class OrderItem extends BaseEntity {

    /**
     * Pedido ao qual este item pertence.
     * O JPA usará este objeto para gerenciar a coluna 'order_id'.
     */
    @NotNull(message = "Pedido é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * O campo 'private Long orderId' foi REMOVIDO para evitar mapeamento duplicado
     * e erros de validação.
     */

    /**
     * Produto referenciado neste item.
     * O JPA usará este objeto para gerenciar a coluna 'product_id'.
     */
    @NotNull(message = "Produto é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * O campo 'private Long productId' foi REMOVIDO para simplificar o mapeamento.
     * Para obter o ID, use 'product.getId()'.
     */

    /**
     * Quantidade do produto neste item.
     * Deve ser maior que zero.
     */
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Preço unitário do produto NO MOMENTO DA COMPRA.
     * <p>
     * IMPORTANTE: Este é um snapshot histórico!
     * - Não muda mesmo se o preço do produto mudar depois
     * - Garante integridade do valor do pedido
     * - Permite análises históricas de precificação
     */
    @NotNull(message = "Preço unitário é obrigatória")
    @DecimalMin(value = "0.0", inclusive = true, message = "Preço unitário não pode ser negativo")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Subtotal do item (quantity × unitPrice).
     * Calculado automaticamente antes de persistir.
     */
    @NotNull(message = "Subtotal é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true, message = "Subtotal não pode ser negativo")
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    // ========================================
    // CONSTRUTORES
    // ========================================

    public OrderItem() {
    }

    /**
     * Construtor de conveniência para criar item com cálculo automático de subtotal.
     *
     * @param product   O objeto Product
     * @param quantity  Quantidade
     * @param unitPrice Preço unitário no momento da compra
     */
    public OrderItem(Product product, Integer quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = calculateSubtotal();
    }

    // ========================================
    // MÉTODOS DE NEGÓCIO
    // ========================================

    /**
     * Calcula o subtotal do item (quantity × unitPrice).
     *
     * @return Subtotal calculado
     */
    public BigDecimal calculateSubtotal() {
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Callback JPA executado antes de persistir (INSERT).
     * Garante que subtotal está calculado corretamente.
     */
    @PrePersist
    public void prePersist() {
        if (subtotal == null) {
            this.subtotal = calculateSubtotal();
        }
    }

    /**
     * Callback JPA executado antes de atualizar (UPDATE).
     * Recalcula subtotal se quantidade ou preço mudarem.
     */
    @PreUpdate
    public void preUpdate() {
        this.subtotal = calculateSubtotal();
    }


    // Se você não usar @Data, descomente e ajuste o toString para usar os objetos
    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + getId() +
                ", orderId=" + (order != null ? order.getId() : null) + // Usa o ID do objeto
                ", productId=" + (product != null ? product.getId() : null) + // Usa o ID do objeto
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                '}';
    }
}