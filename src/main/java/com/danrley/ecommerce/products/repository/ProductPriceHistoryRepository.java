package com.danrley.ecommerce.products.repository;

import com.danrley.ecommerce.products.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para operações de acesso a dados da entidade ProductPriceHistory.
 *
 * Queries especiais para analytics e auditoria:
 * - Histórico de preços de um produto
 * - Produtos com mais mudanças de preço (volatilidade)
 * - Mudanças em um período específico
 * - Última mudança de preço
 *
 * Relacionado ao ADR-004: Auditoria Seletiva
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {

    /**
     * Busca histórico de mudanças de preço de um produto específico.
     * Ordenado por data (mais recente primeiro).
     *
     * Caso de uso: "Mostrar histórico de preços do produto X"
     *
     * @param productId ID do produto
     * @return Lista de mudanças de preço do produto
     */
    List<ProductPriceHistory> findByProductIdOrderByChangedAtDesc(Long productId);

    /**
     * Busca a última mudança de preço de um produto.
     *
     * Caso de uso: "Quando foi a última vez que o preço deste produto mudou?"
     *
     * @param productId ID do produto
     * @return Última mudança de preço (ou null se nunca mudou)
     */
    @Query("SELECT pph FROM ProductPriceHistory pph WHERE pph.productId = :productId ORDER BY pph.changedAt DESC LIMIT 1")
    ProductPriceHistory findLatestByProductId(@Param("productId") Long productId);

    /**
     * Busca mudanças de preço em um período específico.
     *
     * Caso de uso: "Quais produtos tiveram mudança de preço em outubro?"
     *
     * @param startDate Data inicial
     * @param endDate Data final
     * @return Lista de mudanças no período
     */
    @Query("SELECT pph FROM ProductPriceHistory pph WHERE pph.changedAt BETWEEN :startDate AND :endDate ORDER BY pph.changedAt DESC")
    List<ProductPriceHistory> findByChangedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Conta quantas mudanças de preço um produto teve.
     *
     * Caso de uso: "Qual produto teve maior número de mudanças de preço?"
     *
     * @param productId ID do produto
     * @return Quantidade de mudanças
     */
    Long countByProductId(Long productId);

    /**
     * Busca produtos com mais de N mudanças de preço em um período.
     * Útil para identificar produtos com alta volatilidade de preço.
     *
     * Caso de uso: "Produtos em promoção constante (possível problema de margem)"
     *
     * @param startDate Data inicial
     * @param endDate Data final
     * @param minChanges Número mínimo de mudanças
     * @return Lista de productIds com alta volatilidade
     */
    @Query("""
        SELECT pph.productId 
        FROM ProductPriceHistory pph 
        WHERE pph.changedAt BETWEEN :startDate AND :endDate 
        GROUP BY pph.productId 
        HAVING COUNT(pph.id) >= :minChanges
        ORDER BY COUNT(pph.id) DESC
        """)
    List<Long> findHighVolatilityProducts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minChanges") Long minChanges
    );

    /**
     * Busca mudanças realizadas por um usuário específico.
     *
     * Caso de uso: Auditoria - "Quais mudanças de preço o admin X fez?"
     *
     * @param changedBy Email do usuário
     * @return Lista de mudanças realizadas pelo usuário
     */
    List<ProductPriceHistory> findByChangedByOrderByChangedAtDesc(String changedBy);

    /**
     * Busca mudanças de preço com um motivo específico.
     *
     * Caso de uso: "Quais produtos estão em promoção?"
     *
     * @param reason Motivo (ex: "Promoção", "Black Friday")
     * @return Lista de mudanças com o motivo especificado
     */
    @Query("SELECT pph FROM ProductPriceHistory pph WHERE LOWER(pph.reason) LIKE LOWER(CONCAT('%', :reason, '%')) ORDER BY pph.changedAt DESC")
    List<ProductPriceHistory> findByReasonContaining(@Param("reason") String reason);

    /**
     * Busca mudanças de preço que foram aumentos (new > old).
     *
     * Caso de uso: "Produtos que tiveram aumento de preço no último mês"
     *
     * @param startDate Data inicial
     * @return Lista de aumentos de preço
     */
    @Query("SELECT pph FROM ProductPriceHistory pph WHERE pph.newPrice > pph.oldPrice AND pph.changedAt >= :startDate ORDER BY pph.changedAt DESC")
    List<ProductPriceHistory> findPriceIncreases(@Param("startDate") LocalDateTime startDate);

    /**
     * Busca mudanças de preço que foram reduções (new < old).
     *
     * Caso de uso: "Produtos que entraram em promoção no último mês"
     *
     * @param startDate Data inicial
     * @return Lista de reduções de preço
     */
    @Query("SELECT pph FROM ProductPriceHistory pph WHERE pph.newPrice < pph.oldPrice AND pph.changedAt >= :startDate ORDER BY pph.changedAt DESC")
    List<ProductPriceHistory> findPriceDecreases(@Param("startDate") LocalDateTime startDate);
}