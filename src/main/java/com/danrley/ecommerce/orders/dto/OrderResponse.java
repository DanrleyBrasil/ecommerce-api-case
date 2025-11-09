package com.danrley.ecommerce.orders.dto;

import com.danrley.ecommerce.shared.enums.OrderStatus;
import com.danrley.ecommerce.shared.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta contendo dados completos de um pedido.
 *
 * <p>Retornado em operações de:</p>
 * <ul>
 *   <li>Criação de pedido (POST /api/orders)</li>
 *   <li>Consulta por ID (GET /api/orders/{id})</li>
 *   <li>Listagem de pedidos (GET /api/orders)</li>
 * </ul>
 *
 * <p><strong>Informações de Auditoria:</strong></p>
 * <ul>
 *   <li>orderDate: quando foi criado</li>
 *   <li>paymentDate: quando foi pago (null se PENDENTE)</li>
 *   <li>reservedUntil: quando expira a reserva (null se não PENDENTE)</li>
 * </ul>
 *
 * @see com.danrley.ecommerce.orders.entity.Order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    /**
     * ID único do pedido.
     */
    private Long id;

    /**
     * ID do usuário que fez o pedido.
     */
    private Long userId;

    /**
     * Status atual do pedido.
     *
     * <p>Valores possíveis:</p>
     * <ul>
     *   <li>PENDENTE: aguardando pagamento</li>
     *   <li>APROVADO: pago e estoque baixado</li>
     *   <li>CANCELADO: cancelado manualmente</li>
     *   <li>EXPIRED: expirou (reserva liberada)</li>
     * </ul>
     */
    private OrderStatus status;

    /**
     * Valor total do pedido.
     *
     * <p>Calculado como: Σ (item.quantity × item.unitPrice)</p>
     */
    private BigDecimal totalAmount;

    /**
     * Data/hora de criação do pedido.
     */
    private LocalDateTime orderDate;

    /**
     * Data/hora do pagamento.
     *
     * <p>Null enquanto status = PENDENTE.</p>
     */
    private LocalDateTime paymentDate;

    /**
     * Data/hora limite da reserva (TTL).
     *
     * <p>Após esse horário, job agendado expira o pedido
     * automaticamente e libera o estoque reservado.</p>
     *
     * <p>Null para pedidos APROVADO/CANCELADO/EXPIRED.</p>
     */
    private LocalDateTime reservedUntil;

    /**
     * Lista de itens do pedido.
     */
    private List<OrderItemResponse> items;

    /**
     * Timestamps de auditoria.
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}