package com.danrley.ecommerce.shared.enums;

/**
 * Status possíveis de um pedido no sistema.
 * <p>
 * Representa o ciclo de vida de um pedido:
 * <ul>
 *   <li>PENDENTE - Pedido criado, aguardando pagamento (reserva ativa por 10min)</li>
 *   <li>APROVADO - Pagamento confirmado, estoque atualizado</li>
 *   <li>CANCELADO - Pedido cancelado manualmente pelo usuário ou admin</li>
 *   <li>EXPIRED - Pedido expirado automaticamente (reserva ultrapassou TTL de 10min)</li>
 * </ul>
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 * @see com.danrley.ecommerce.orders.entity.Order
 */
public enum OrderStatus {
    PENDENTE,
    APROVADO,
    CANCELADO,
    EXPIRED  // ⭐ Status para pedidos com reserva expirada
}