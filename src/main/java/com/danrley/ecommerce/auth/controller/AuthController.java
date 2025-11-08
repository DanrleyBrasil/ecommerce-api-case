package com.danrley.ecommerce.auth.controller;

import com.danrley.ecommerce.auth.dto.AuthResponse;
import com.danrley.ecommerce.auth.dto.LoginRequest;
import com.danrley.ecommerce.auth.dto.RegisterRequest;
import com.danrley.ecommerce.auth.entity.User;
import com.danrley.ecommerce.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticação.
 *
 * Endpoints:
 * - POST /api/auth/register - Registro de novo usuário (público)
 * - POST /api/auth/login - Login (público)
 * - GET /api/auth/me - Dados do usuário autenticado (protegido)
 *
 * @see AuthService
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro")
public class AuthController {

    private final AuthService authService;

    /**
     * Registra um novo usuário.
     *
     * Endpoint público. Cria usuário com role USER por padrão.
     *
     * @param request dados do novo usuário
     * @return 201 Created com token JWT
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", description = "Cria novo usuário com role USER")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Autentica usuário e retorna token JWT.
     *
     * Endpoint público.
     *
     * @param request credenciais de login
     * @return 200 OK com token JWT
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica usuário e retorna token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna dados do usuário autenticado.
     *
     * Endpoint protegido. Requer token JWT válido.
     *
     * @param user usuário autenticado (injetado pelo Spring Security)
     * @return 200 OK com dados do usuário
     */
    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado", description = "Retorna dados do usuário logado")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }
}