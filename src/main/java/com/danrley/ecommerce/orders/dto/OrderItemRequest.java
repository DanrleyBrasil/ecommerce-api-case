package com.danrley.ecommerce.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para item individual de um pedido.
 *
 * <p>Representa um produto e sua quantidade no carrinho.
 * O preço unitário (unitPrice) será capturado automaticamente
 * do produto no momento da criação (snapshot histórico).</p>
 *
 * <p><strong>Validações:</strong></p>
 * <ul>
 *   <li>productId obrigatório</li>
 *   <li>quantity mínima = 1</li>
 * </ul>
 *
 * <p><strong>Exemplo de uso:</strong></p>
 * <pre>
 * {
 *   "productId": 1,
 *   "quantity": 2
 * }
 * </pre>
 *
 * @see com.danrley.ecommerce.orders.entity.OrderItem
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    /**
     * ID do produto a ser comprado.
     *
     * <p>Deve existir na tabela products e estar ativo (active = true).</p>
     */
    @NotNull(message = "ID do produto é obrigatório")
    private Long productId;

    /**
     * Quantidade desejada.
     *
     * <p>Será validada contra o estoque disponível:
     * stockQuantity - reservedQuantity >= quantity</p>
     */
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade mínima é 1")
    private Integer quantity;
}