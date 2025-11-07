package com.danrley.ecommerce.auth.repository;

import com.danrley.ecommerce.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório JPA para acesso aos dados de {@link User}.
 * <p>
 * Fornece operações de persistência e consultas customizadas para autenticação
 * e gerenciamento de usuários.
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Busca um usuário pelo email único.
     * <p>
     * Utilizado durante o processo de autenticação JWT.
     * </p>
     *
     * @param email email do usuário
     * @return Optional contendo o usuário se encontrado
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se já existe um usuário cadastrado com o email informado.
     * <p>
     * Utilizado para validação durante registro de novos usuários.
     * </p>
     *
     * @param email email a ser verificado
     * @return true se email já existe, false caso contrário
     */
    Boolean existsByEmail(String email);
}