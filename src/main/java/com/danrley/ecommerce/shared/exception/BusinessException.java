package com.danrley.ecommerce.shared.exception;

/**
 * Exceção base para todas as regras de negócio da aplicação.
 * Estende RuntimeException para não forçar try-catch em todo lugar.
 *
 * Utilizada quando uma regra de negócio é violada.
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}