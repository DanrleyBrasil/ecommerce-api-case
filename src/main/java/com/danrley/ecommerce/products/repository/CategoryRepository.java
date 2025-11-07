package com.danrley.ecommerce.products.repository;

import com.danrley.ecommerce.products.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
/**
 * Repositório JPA para acesso aos dados de {@link Category}.
 * <p>
 * Fornece operações de persistência e consultas para categorias de produtos.
 * </p>
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Busca todas as categorias ativas no sistema.
     *
     * @return lista de categorias com active=true
     */
    List<Category> findByActiveTrue();
}
