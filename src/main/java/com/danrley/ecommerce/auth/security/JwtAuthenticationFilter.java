package com.danrley.ecommerce.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que intercepta TODAS as requests HTTP.
 *
 * Responsabilidades:
 * 1. Extrair token JWT do header Authorization
 * 2. Validar token (assinatura, expiração)
 * 3. Carregar User do banco
 * 4. Configurar SecurityContext (autenticação)
 *
 * Fluxo:
 * 1. Request chega → JwtAuthenticationFilter
 * 2. Extrai "Authorization: Bearer <token>"
 * 3. Valida token via JwtService
 * 4. Busca User via UserDetailsService
 * 5. Configura SecurityContextHolder
 * 6. Controller recebe User autenticado
 *
 * Endpoints públicos (sem token):
 * - /api/auth/login
 * - /api/auth/register
 * - /swagger-ui/**
 * - /v3/api-docs/**
 *
 * @see JwtService
 * @see UserDetailsServiceImpl
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extrair header Authorization
        final String authHeader = request.getHeader("Authorization");

        // Se não tem header ou não começa com "Bearer ", pula filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extrair token (remover "Bearer " do início)
        final String jwt = authHeader.substring(7);

        try {
            // 3. Extrair email do token
            final String userEmail = jwtService.extractUsername(jwt);

            // 4. Se email existe e usuário não está autenticado ainda
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. Buscar User do banco
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // 6. Validar token
                if (jwtService.isTokenValid(jwt, (com.danrley.ecommerce.auth.entity.User) userDetails)) {

                    // 7. Criar objeto de autenticação
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 8. Configurar SecurityContext (usuário autenticado)
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token inválido/expirado - não autentica (401 será retornado)
            logger.error("JWT validation error: " + e.getMessage());
        }

        // 9. Continuar cadeia de filtros
        filterChain.doFilter(request, response);
    }
}