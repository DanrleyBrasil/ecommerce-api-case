package com.danrley.ecommerce.shared.exception;

/**
 * Exceção lançada quando um recurso solicitado não é encontrado no banco de dados.
 *
 * Exemplos:
 * - Usuário não encontrado por ID ou email
 * - Produto não encontrado por ID ou SKU
 * - Pedido não encontrado por ID
 * - Categoria não encontrada por nome
 *
 * Retorna HTTP 404 Not Found quando tratada pelo GlobalExceptionHandler.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Construtor principal com detalhes do recurso não encontrado.
     *
     * @param resourceName Nome do recurso (ex: "User", "Product", "Order")
     * @param fieldName Nome do campo de busca (ex: "id", "email", "sku")
     * @param fieldValue Valor buscado
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s não encontrado(a) com %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * Construtor simplificado com mensagem customizada.
     *
     * @param message Mensagem de erro customizada
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = "Resource";
        this.fieldName = "unknown";
        this.fieldValue = null;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}