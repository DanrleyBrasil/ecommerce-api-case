package com.danrley.ecommerce.products.repository;

import com.danrley.ecommerce.products.entity.Category;
import com.danrley.ecommerce.products.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de produtos com suporte a lock pessimista para controle de estoque.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Busca produto pelo código SKU único.
     */
    Optional<Product> findBySku(String sku);

    /**
     * Lista produtos ativos de uma categoria com paginação.
     */
    List<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    /**
     * Busca produto com lock pessimista (FOR UPDATE).
     * Usado durante processamento de pagamento para evitar race conditions.
     * Outros processos aguardam até o lock ser liberado (ADR-003).
     *
     * @param id identificador do produto
     * @return produto com lock exclusivo na transação
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    /**
     * Verifica se já existe um produto com o SKU informado.
     *
     * @param sku Código SKU a ser verificado.
     * @return true se o SKU já existir, false caso contrário.
     */
    boolean existsBySku(String sku); // <--- ADICIONE ESTA LINHA
}