package com.danrley.ecommerce.auth.entity;

import com.danrley.ecommerce.shared.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
@NoArgsConstructor // ✅ NECESSÁRIO para JPA
@AllArgsConstructor // ✅ NECESSÁRIO para @Builder
@Builder // ✅ ADICIONAR ESTA ANOTAÇÃO!
@Entity
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    @Builder.Default // ✅ IMPORTANTE: Define valor padrão no builder
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default // ✅ IMPORTANTE: Define valor padrão no builder
    private Set<Role> roles = new HashSet<>();

    // ========================================
    // Implementação de UserDetails (Spring Security)
    // ========================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return email; // Spring Security usa email como username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}