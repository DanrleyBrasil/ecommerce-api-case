package com.danrley.ecommerce.shared.exception;

import com.danrley.ecommerce.shared.enums.OrderStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exceção lançada quando uma operação é tentada em um pedido com status inválido.
 *
 * <p><strong>Casos de uso:</strong></p>
 * <ol>
 *   <li>Tentar processar pagamento de pedido já APROVADO ou CANCELADO</li>
 *   <li>Tentar cancelar pedido já APROVADO</li>
 *   <li>Tentar reprocessar pedido EXPIRADO</li>
 * </ol>
 *
 * <p>Relacionado ao ADR-003: Controle de status de pedidos com reserva temporária.</p>
 *
 * <p>Retorna HTTP 400 Bad Request quando tratada pelo GlobalExceptionHandler.</p>
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
     * Factory method para indicar que pedido já foi processado.
     *
     * @param orderId ID do pedido
     * @param currentStatus Status atual do pedido
     * @return Exceção configurada
     */
    public static InvalidOrderStatusException alreadyProcessed(Long orderId, OrderStatus currentStatus) {
        return new InvalidOrderStatusException(
                orderId,
                String.format("Pedido %d já foi processado. Status atual: %s", orderId, currentStatus)
        );
    }

    /**
     * Factory method para indicar que pedido expirou.
     *
     * @param orderId ID do pedido
     * @return Exceção configurada
     */
    public static InvalidOrderStatusException expired(Long orderId) {
        return new InvalidOrderStatusException(
                orderId,
                String.format("Pedido %d expirou. A reserva de estoque foi liberada.", orderId)
        );
    }

    /**
     * Factory method para indicar que pedido expirou (com informação de quando).
     *
     * <p>Versão mais informativa que inclui a data/hora de expiração
     * para melhor rastreabilidade e UX.</p>
     *
     * @param orderId ID do pedido
     * @param reservedUntil Data/hora que a reserva expirou
     * @return Exceção configurada
     */
    public static InvalidOrderStatusException expired(Long orderId, LocalDateTime reservedUntil) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDate = reservedUntil != null ? reservedUntil.format(formatter) : "N/A";

        return new InvalidOrderStatusException(
                orderId,
                String.format(
                        "Pedido %d expirou em %s. A reserva de estoque foi liberada. Crie um novo pedido.",
                        orderId,
                        formattedDate
                )
        );
    }

    // Getters
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