package com.danrley.ecommerce.config;

import com.danrley.ecommerce.shared.entity.BaseEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Configuração do JPA Auditing para preenchimento automático de campos de auditoria.
 * <p>
 * Habilita a auditoria automática nas entidades que herdam de {@link BaseEntity},
 * preenchendo automaticamente os campos createdBy e updatedBy.
 * </p>
 * <p>
 * Atualmente retorna "system" como auditor padrão. Será refinado no MACRO 6
 * para extrair o usuário autenticado do contexto de segurança JWT.
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 * @see BaseEntity
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Provedor de auditoria que retorna o identificador do usuário atual.
     * <p>
     * Implementação temporária que retorna "system" para todos os registros.
     * Será substituída por implementação que extrai o usuário do SecurityContext
     * após implementação do JWT (MACRO 6).
     * </p>
     *
     * @return AuditorAware que retorna "system"
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        // Mock por enquanto - retorna "system"
        return () -> Optional.of("system");
    }
}
