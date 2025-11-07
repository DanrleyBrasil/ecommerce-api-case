package com.danrley.ecommerce.shared.entity;

import com.danrley.ecommerce.config.JpaAuditingConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Classe base abstrata para todas as entidades do sistema.
 * <p>
 * Fornece campos de auditoria automática que são preenchidos pelo Spring Data JPA:
 * <ul>
 *   <li>id - Identificador único gerado automaticamente</li>
 *   <li>createdAt - Data/hora de criação (imutável)</li>
 *   <li>updatedAt - Data/hora da última modificação (atualizada automaticamente)</li>
 *   <li>createdBy - Usuário que criou o registro</li>
 *   <li>updatedBy - Usuário que fez a última modificação</li>
 * </ul>
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 * @see JpaAuditingConfig
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
