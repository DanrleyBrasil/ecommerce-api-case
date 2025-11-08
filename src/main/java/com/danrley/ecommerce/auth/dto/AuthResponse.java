package com.danrley.ecommerce.auth.dto;

import com.danrley.ecommerce.shared.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response de autenticação contendo token JWT e dados do usuário.
 *
 * Campos:
 * - token: JWT válido por 24h
 * - type: Tipo do token (sempre "Bearer")
 * - userId: ID do usuário autenticado
 * - name: Nome do usuário
 * - email: Email do usuário
 * - roles: Roles do usuário (ADMIN, USER)
 *
 * @see com.danrley.ecommerce.auth.service.AuthService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private Long userId;
    private String name;
    private String email;
    private Set<UserRole> roles;
}