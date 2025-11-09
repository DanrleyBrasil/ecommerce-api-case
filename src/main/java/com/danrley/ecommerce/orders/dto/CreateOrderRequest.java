package com.danrley.ecommerce.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para criação de pedidos.
 *
 * <p>Contém lista de itens que o cliente deseja comprar.
 * O userId será extraído do token JWT automaticamente.</p>
 *
 * <p><strong>Validações:</strong></p>
 * <ul>
 *   <li>Lista de itens não pode ser vazia</li>
 *   <li>Cada item deve ser válido (ver OrderItemRequest)</li>
 * </ul>
 *
 * <p><strong>Fluxo de Negócio (ADR-003):</strong></p>
 * <ol>
 *   <li>Validar estoque de TODOS os produtos</li>
 *   <li>Se OK → Reservar temporariamente (reserved_quantity)</li>
 *   <li>Criar Order com status PENDENTE</li>
 *   <li>Setar reservedUntil = NOW + 10 minutos</li>
 * </ol>
 *
 * @see com.danrley.ecommerce.orders.dto.OrderItemRequest
 * @see com.danrley.ecommerce.orders.service.OrderService#createOrder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    /**
     * Lista de itens do pedido.
     *
     * <p>Deve conter ao menos 1 item. Cada item será validado
     * individualmente (productId e quantity obrigatórios).</p>
     */
    @NotEmpty(message = "O pedido deve conter ao menos um item")
    @Valid
    private List<com.danrley.ecommerce.orders.dto.OrderItemRequest> items;
}