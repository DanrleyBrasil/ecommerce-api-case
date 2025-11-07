package com.danrley.ecommerce.auth.repository;

import com.danrley.ecommerce.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório JPA para acesso aos dados de {@link Role}.
 * <p>
 * Fornece operações de persistência e consultas customizadas para roles do sistema.
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Busca uma role pelo nome único.
     *
     * @param name nome da role (ex: "ADMIN", "USER")
     * @return Optional contendo a role se encontrada
     */
    Optional<Role> findByName(String name);
}
