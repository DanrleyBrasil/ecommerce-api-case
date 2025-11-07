package com.danrley.ecommerce.auth.entity;

import com.danrley.ecommerce.shared.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidade que representa um usuário do sistema.
 * <p>
 * Implementa autenticação e controle de acesso através de relacionamento N:N
 * com {@link Role} (RBAC - Role-Based Access Control).
 * </p>
 * <p>
 * O password é armazenado como hash BCrypt para segurança.
 * A relação com roles utiliza fetch EAGER para evitar LazyInitializationException
 * durante autenticação JWT.
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @see Role
 * @see BaseEntity
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}