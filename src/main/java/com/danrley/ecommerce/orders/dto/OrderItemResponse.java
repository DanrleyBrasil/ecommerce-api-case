package com.danrley.ecommerce.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de resposta para item individual de um pedido.
 *
 * <p>Contém informações completas do produto no momento da compra,
 * incluindo snapshot do preço (unitPrice).</p>
 *
 * <p><strong>Por que armazenar unitPrice?</strong></p>
 * <p>O preço do produto pode mudar ao longo do tempo. Para manter
 * histórico preciso, armazenamos o preço no momento da compra.</p>
 *
 * @see com.danrley.ecommerce.orders.entity.OrderItem
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    /**
     * ID único do item.
     */
    private Long id;

    /**
     * ID do produto.
     */
    private Long productId;

    /**
     * Nome do produto (no momento da compra).
     */
    private String productName;

    /**
     * Quantidade comprada.
     */
    private Integer quantity;

    /**
     * Preço unitário no momento da compra (snapshot).
     *
     * <p>Mesmo que o produto mude de preço depois,
     * este valor permanece inalterado.</p>
     */
    private BigDecimal unitPrice;

    /**
     * Subtotal do item.
     *
     * <p>Calculado como: quantity × unitPrice</p>
     */
    private BigDecimal subtotal;
}