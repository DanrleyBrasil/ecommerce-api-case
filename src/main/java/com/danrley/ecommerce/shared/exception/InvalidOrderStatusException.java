package com.danrley.ecommerce.shared.exception;

import com.danrley.ecommerce.shared.enums.OrderStatus;

/**
 * Exceção lançada quando uma operação é tentada em um pedido com status inválido.
 *
 * Casos de uso:
 * 1. Tentar processar pagamento de pedido já APROVADO ou CANCELADO
 * 2. Tentar cancelar pedido já APROVADO
 * 3. Tentar reprocessar pedido EXPIRADO
 *
 * Relacionado ao ADR-003: Controle de status de pedidos com reserva temporária
 *
 * Retorna HTTP 400 Bad Request quando tratada pelo GlobalExceptionHandler.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
public class InvalidOrderStatusException extends BusinessException {

    private final Long orderId;
    private final OrderStatus currentStatus;
    private final OrderStatus expectedStatus;

    /**
     * Construtor com detalhes da transição inválida.
     *
     * @param orderId ID do pedido
     * @param currentStatus Status atual do pedido
     * @param expectedStatus Status esperado para a operação
     */
    public InvalidOrderStatusException(Long orderId, OrderStatus currentStatus, OrderStatus expectedStatus) {
        super(
                String.format(
                        "Operação inválida para o pedido %d. Status atual: %s, Status esperado: %s",
                        orderId, currentStatus, expectedStatus
                ),
                "INVALID_ORDER_STATUS"
        );
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.expectedStatus = expectedStatus;
    }

    /**
     * Construtor simplificado apenas com mensagem customizada.
     *
     * @param orderId ID do pedido
     * @param message Mensagem de erro customizada
     */
    public InvalidOrderStatusException(Long orderId, String message) {
        super(message, "INVALID_ORDER_STATUS");
        this.orderId = orderId;
        this.currentStatus = null;
        this.expectedStatus = null;
    }

    /**
     * Construtor para indicar que pedido já foi processado.
     *
     * @param orderId ID do pedido
     * @param currentStatus Status atual do pedido
     */
    public static InvalidOrderStatusException alreadyProcessed(Long orderId, OrderStatus currentStatus) {
        return new InvalidOrderStatusException(
                orderId,
                String.format("Pedido %d já foi processado. Status atual: %s", orderId, currentStatus)
        );
    }

    /**
     * Construtor para indicar que pedido expirou.
     *
     * @param orderId ID do pedido
     */
    public static InvalidOrderStatusException expired(Long orderId) {
        return new InvalidOrderStatusException(
                orderId,
                String.format("Pedido %d expirou. A reserva de estoque foi liberada.", orderId)
        );
    }

    public Long getOrderId() {
        return orderId;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getExpectedStatus() {
        return expectedStatus;
    }
}