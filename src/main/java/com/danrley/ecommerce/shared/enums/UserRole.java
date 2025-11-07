package com.danrley.ecommerce.shared.enums;

/**
 * Papéis (roles) disponíveis no sistema de autenticação RBAC.
 * <p>
 * Define os níveis de acesso:
 * <ul>
 *   <li>ADMIN - Administrador com acesso total ao sistema</li>
 *   <li>USER - Usuário cliente com acesso às funcionalidades de compra</li>
 * </ul>
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
public enum UserRole {
    ADMIN,
    USER
}
