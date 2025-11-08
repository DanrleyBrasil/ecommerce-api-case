package com.danrley.ecommerce.shared.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Classe base para entidades SEM auditoria de usuário.
 * Usada por entidades de apoio (Category, Supplier) que não precisam
 * rastrear quem criou/modificou.
 *
 * Campos de auditoria:
 * - createdAt (data criação)
 * - updatedAt (data modificação)
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntitySimple {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}