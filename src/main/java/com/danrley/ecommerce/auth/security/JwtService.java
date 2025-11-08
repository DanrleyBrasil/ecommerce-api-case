package com.danrley.ecommerce.auth.security;

import com.danrley.ecommerce.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsável por geração e validação de tokens JWT.
 *
 * Funcionalidades:
 * - Gerar token JWT a partir de User
 * - Extrair informações do token (email, userId, roles)
 * - Validar token (assinatura, expiração, usuário)
 *
 * Algoritmo: HS256 (HMAC with SHA-256)
 * Expiração: 24 horas (configurável via application.yml)
 *
 * Claims customizados:
 * - userId: ID do usuário
 * - roles: Lista de roles do usuário
 *
 * @see JwtAuthenticationFilter
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Gera token JWT a partir de User.
     *
     * Claims incluídos:
     * - subject: email do usuário
     * - userId: ID do usuário
     * - roles: Lista de roles (ADMIN, USER)
     * - issuedAt: Data de emissão
     * - expiration: Data de expiração (24h)
     *
     * @param user usuário autenticado
     * @return token JWT assinado
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));

        return buildToken(extraClaims, user, jwtExpiration);
    }

    /**
     * Constrói token JWT com claims customizados.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            User user,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Valida se token é válido para o usuário.
     *
     * Validações:
     * - Email do token == email do usuário
     * - Token não expirou
     * - Assinatura válida
     *
     * @param token JWT token
     * @param user usuário autenticado
     * @return true se válido, false caso contrário
     */
    public boolean isTokenValid(String token, User user) {
        final String username = extractUsername(token);
        return (username.equals(user.getEmail())) && !isTokenExpired(token);
    }

    /**
     * Extrai email (username) do token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrai claim específico do token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Verifica se token expirou.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrai data de expiração do token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrai todos os claims do token.
     *
     * Lança ExpiredJwtException se token expirado.
     * Lança SignatureException se assinatura inválida.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Obtém chave de assinatura a partir do secret configurado.
     */
    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}