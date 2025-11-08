package com.danrley.ecommerce.shared.exception;

/**
 * Exceção para erros de autenticação (credenciais inválidas, token expirado, etc).
 *
 * HTTP Status: 401 Unauthorized
 *
 * Casos de uso:
 * - Credenciais inválidas no login
 * - Token JWT expirado
 * - Token JWT inválido
 * - Usuário inativo
 *
 * @see com.danrley.ecommerce.auth.service.AuthService
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    // Factory methods para casos comuns
    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Email ou senha inválidos");
    }

    public static AuthenticationException expiredToken() {
        return new AuthenticationException("Token JWT expirado");
    }

    public static AuthenticationException invalidToken() {
        return new AuthenticationException("Token JWT inválido");
    }

    public static AuthenticationException inactiveUser() {
        return new AuthenticationException("Usuário inativo");
    }
}