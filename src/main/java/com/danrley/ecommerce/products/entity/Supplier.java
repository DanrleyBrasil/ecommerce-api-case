package com.danrley.ecommerce.products.entity;


import com.danrley.ecommerce.shared.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

/**
 * Entidade que representa um fornecedor de produtos.
 * <p>
 * Relacionamento 1:N OPCIONAL com {@link Product}.
 * Produtos podem não ter fornecedor cadastrado (supplier_id NULL).
 * </p>
 * <p>
 * CRUD não implementado no escopo atual (ADR-004).
 * Tabela normalizada para demonstrar modelagem profissional.
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 * @see Product
 */
@Getter
@Setter
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 18)
    private String cnpj;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private Boolean active = true;
}
