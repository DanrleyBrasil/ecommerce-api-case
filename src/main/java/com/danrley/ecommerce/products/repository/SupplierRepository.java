package com.danrley.ecommerce.products.repository;

import com.danrley.ecommerce.products.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 * Repositório JPA para acesso aos dados de {@link Supplier}.
 * <p>
 * Fornece operações básicas de persistência. Métodos customizados não implementados
 * pois CRUD de fornecedores não faz parte do escopo atual (ADR-004).
 * </p>
 * <p>
 * Repository mantido para leitura e possível evolução futura.
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    // Read-only por enquanto - sem métodos customizados
}