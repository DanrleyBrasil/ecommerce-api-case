package com.danrley.ecommerce.shared.exception;

/**
 * Exceção lançada quando não há estoque suficiente para processar um pedido.
 *
 * Casos de uso:
 * 1. Criação de pedido: validação inicial de estoque
 * 2. Processamento de pagamento: re-validação com lock pessimista (race condition)
 *
 * Relacionado ao ADR-003: Locks Pessimistas + Reserva Temporária de Estoque
 *
 * Retorna HTTP 409 Conflict quando tratada pelo GlobalExceptionHandler.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
public class InsufficientStockException extends BusinessException {

    private final Long productId;
    private final String productName;
    private final Integer requested;
    private final Integer available;

    /**
     * Construtor com detalhes do estoque insuficiente.
     *
     * @param productId ID do produto
     * @param productName Nome do produto
     * @param requested Quantidade solicitada
     * @param available Quantidade disponível
     */
    public InsufficientStockException(Long productId, String productName, Integer requested, Integer available) {
        super(
                String.format(
                        "Estoque insuficiente para o produto '%s'. Solicitado: %d, Disponível: %d",
                        productName, requested, available
                ),
                "INSUFFICIENT_STOCK"
        );
        this.productId = productId;
        this.productName = productName;
        this.requested = requested;
        this.available = available;
    }

    /**
     * Construtor simplificado apenas com nome do produto.
     *
     * @param productName Nome do produto
     */
    public InsufficientStockException(String productName) {
        super(
                String.format("Estoque insuficiente para o produto '%s'", productName),
                "INSUFFICIENT_STOCK"
        );
        this.productId = null;
        this.productName = productName;
        this.requested = null;
        this.available = null;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getRequested() {
        return requested;
    }

    public Integer getAvailable() {
        return available;
    }
}