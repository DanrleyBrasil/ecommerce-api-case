package com.danrley.ecommerce.products.entity;

import com.danrley.ecommerce.shared.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

/**
 * Entidade que representa uma categoria de produtos no catálogo.
 * <p>
 * Categorias são fixas e pré-definidas no sistema (5 categorias).
 * Relacionamento 1:N com {@link Product}.
 * </p>
 * <p>
 * CRUD limitado: apenas listagem (GET) implementada.
 * Criação/edição de categorias não faz parte do escopo atual (ADR-004).
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 * @see Product
 */
@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;
}
