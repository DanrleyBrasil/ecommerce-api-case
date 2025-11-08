package com.danrley.ecommerce.auth.service;

import com.danrley.ecommerce.auth.dto.AuthResponse;
import com.danrley.ecommerce.auth.dto.LoginRequest;
import com.danrley.ecommerce.auth.dto.RegisterRequest;
import com.danrley.ecommerce.auth.entity.Role;
import com.danrley.ecommerce.auth.entity.User;
import com.danrley.ecommerce.auth.repository.RoleRepository;
import com.danrley.ecommerce.auth.repository.UserRepository;
import com.danrley.ecommerce.auth.security.JwtService;
import com.danrley.ecommerce.shared.exception.AuthenticationException;
import com.danrley.ecommerce.shared.exception.BusinessException;
import com.danrley.ecommerce.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de autenticação responsável por login e registro de usuários.
 *
 * Funcionalidades:
 * - Registro de novos usuários (role USER por padrão)
 * - Login com validação de credenciais
 * - Geração de token JWT
 *
 * @see JwtService
 * @see com.danrley.ecommerce.auth.controller.AuthController
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registra um novo usuário no sistema.
     *
     * Processo:
     * 1. Valida se email já existe
     * 2. Cria usuário com senha criptografada (BCrypt)
     * 3. Atribui role USER por padrão
     * 4. Gera token JWT
     *
     * @param request dados do novo usuário
     * @return AuthResponse com token e dados do usuário
     * @throws BusinessException se email já existe
     * @throws ResourceNotFoundException se role USER não existe
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validar se email já existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado", "EMAIL_ALREADY_EXISTS");
        }

        // Buscar role USER (deve existir no banco via dump.sql)
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "USER"));

        // Criar usuário
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt
                .active(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        user = userRepository.save(user);

        // Gerar token JWT
        String token = jwtService.generateToken(user);

        return buildAuthResponse(user, token);
    }

    /**
     * Autentica usuário e gera token JWT.
     *
     * Processo:
     * 1. Busca usuário por email
     * 2. Valida se usuário está ativo
     * 3. Valida senha (BCrypt)
     * 4. Gera token JWT
     *
     * @param request credenciais de login
     * @return AuthResponse com token e dados do usuário
     * @throws AuthenticationException se credenciais inválidas ou usuário inativo
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Buscar usuário por email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(AuthenticationException::invalidCredentials);

        // Validar se usuário está ativo
        if (!user.getActive()) {
            throw AuthenticationException.inactiveUser();
        }

        // Validar senha
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw AuthenticationException.invalidCredentials();
        }

        // Gerar token JWT
        String token = jwtService.generateToken(user);

        return buildAuthResponse(user, token);
    }

    /**
     * Constrói AuthResponse a partir de User e token.
     */
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(role -> com.danrley.ecommerce.shared.enums.UserRole.valueOf(role.getName()))
                        .collect(Collectors.toSet()))
                .build();
    }
}