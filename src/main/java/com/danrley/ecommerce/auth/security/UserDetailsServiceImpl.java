package com.danrley.ecommerce.auth.security;

import com.danrley.ecommerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementação customizada de UserDetailsService para Spring Security.
 *
 * Responsável por buscar User do banco de dados durante:
 * - Login (AuthenticationManager)
 * - Validação de token JWT (JwtAuthenticationFilter)
 *
 * Spring Security usa esta classe para carregar usuário por email.
 *
 * @see JwtAuthenticationFilter
 * @see com.danrley.ecommerce.auth.config.SecurityConfig
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carrega User do banco por email.
     *
     * Spring Security chama este método automaticamente durante autenticação.
     *
     * @param email email do usuário
     * @return UserDetails (nossa classe User implementa esta interface)
     * @throws UsernameNotFoundException se usuário não existe
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado com email: " + email
                ));
    }
}