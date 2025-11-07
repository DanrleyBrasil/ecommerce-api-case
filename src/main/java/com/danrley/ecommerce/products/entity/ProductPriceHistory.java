package com.danrley.ecommerce.products.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade para auditoria seletiva de mudanças de preço de produtos.
 *
 * Relacionado ao ADR-004: Auditoria Seletiva - Histórico de Preços
 *
 * Por que auditar APENAS preços?
 * - Mudanças de preço são MANUAIS e têm impacto comercial direto
 * - Volume de dados BAIXO (~10-20 mudanças/mês)
 * - Valor analítico REAL (volatilidade, promoções, impacto de margem)
 * - Compliance: rastrear quem/quando/por que mudou preços
 *
 * Por que NÃO auditar estoque?
 * - Volume GIGANTESCO (milhões de registros em eventos como Black Friday)
 * - Redundância: order_items já rastreia vendas (99% das movimentações)
 * - Sem valor analítico proporcional ao custo de armazenamento
 *
 * IMPORTANTE: Esta entidade NÃO herda de BaseEntity!
 * - Auditoria customizada (changed_by, changed_at)
 * - Não precisa de created_by, updated_by (registro imutável)
 *
 * Casos de uso:
 * 1. Analytics: "Qual produto teve maior volatilidade nos últimos 30 dias?"
 * 2. Auditoria: "Quem aprovou essa promoção? Quando?"
 * 3. Histórico: "Qual foi o preço deste produto em outubro?"
 * 4. Compliance: Justificar mudanças de preço para fornecedores/auditoria
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 * @see Product
 * @see com.danrley.ecommerce.products.repository.ProductPriceHistoryRepository
 */
@Data
@Entity
@Table(name = "product_price_history", indexes = {
        @Index(name = "idx_product_price_product", columnList = "product_id"),
        @Index(name = "idx_product_price_date", columnList = "changed_at")
})
public class ProductPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID do produto cujo preço foi alterado.
     * Relacionamento Many-to-One com Product.
     */
    @NotNull(message = "Produto é obrigatório")
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    /**
     * Preço anterior (antes da mudança).
     */
    @NotNull(message = "Preço anterior é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true, message = "Preço anterior não pode ser negativo")
    @Column(name = "old_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal oldPrice;

    /**
     * Novo preço (após a mudança).
     */
    @NotNull(message = "Novo preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true, message = "Novo preço não pode ser negativo")
    @Column(name = "new_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal newPrice;

    /**
     * Usuário que realizou a mudança de preço.
     * Formato: email do usuário (ex: admin@ecommerce.com)
     */
    @NotBlank(message = "Usuário que alterou é obrigatório")
    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    /**
     * Data/hora em que a mudança foi realizada.
     * Preenchida automaticamente no momento da criação.
     */
    @NotNull
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /**
     * Motivo da mudança de preço.
     *
     * Exemplos:
     * - "Promoção de Black Friday"
     * - "Ajuste de margem - competição"
     * - "Redução sazonal - baixa demanda"
     * - "Reajuste de fornecedor"
     *
     * Campo opcional, mas altamente recomendado para compliance.
     */
    @Column(name = "reason", length = 255)
    private String reason;

    // ========================================
    // CONSTRUTORES
    // ========================================

    public ProductPriceHistory() {
        this.changedAt = LocalDateTime.now();
    }

    /**
     * Construtor de conveniência para criar histórico de preço.
     *
     * @param productId ID do produto
     * @param oldPrice Preço anterior
     * @param newPrice Novo preço
     * @param changedBy Usuário que alterou
     */
    public ProductPriceHistory(Long productId, BigDecimal oldPrice, BigDecimal newPrice, String changedBy) {
        this.productId = productId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    /**
     * Construtor completo com motivo.
     *
     * @param productId ID do produto
     * @param oldPrice Preço anterior
     * @param newPrice Novo preço
     * @param changedBy Usuário que alterou
     * @param reason Motivo da mudança
     */
    public ProductPriceHistory(Long productId, BigDecimal oldPrice, BigDecimal newPrice, String changedBy, String reason) {
        this(productId, oldPrice, newPrice, changedBy);
        this.reason = reason;
    }

    // ========================================
    // MÉTODOS DE NEGÓCIO
    // ========================================

    /**
     * Calcula a diferença entre o novo preço e o antigo.
     *
     * @return Diferença (positiva = aumento, negativa = redução)
     */
    public BigDecimal getPriceDifference() {
        return newPrice.subtract(oldPrice);
    }

    /**
     * Calcula a variação percentual do preço.
     *
     * @return Percentual de variação (ex: 10.50 para aumento de 10.5%)
     */
    public BigDecimal getPercentageChange() {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal difference = getPriceDifference();
        return difference.divide(oldPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Verifica se foi um aumento de preço.
     *
     * @return true se newPrice > oldPrice
     */
    public boolean isPriceIncrease() {
        return newPrice.compareTo(oldPrice) > 0;
    }

    /**
     * Verifica se foi uma redução de preço.
     *
     * @return true se newPrice < oldPrice
     */
    public boolean isPriceDecrease() {
        return newPrice.compareTo(oldPrice) < 0;
    }

    /**
     * Callback JPA executado antes de persistir.
     * Garante que changedAt está preenchido.
     */
    @PrePersist
    public void prePersist() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
        }
    }

    // ========================================
    // EQUALS, HASHCODE, TOSTRING
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductPriceHistory)) return false;
        ProductPriceHistory that = (ProductPriceHistory) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProductPriceHistory{" +
                "id=" + id +
                ", productId=" + productId +
                ", oldPrice=" + oldPrice +
                ", newPrice=" + newPrice +
                ", changedBy='" + changedBy + '\'' +
                ", changedAt=" + changedAt +
                ", reason='" + reason + '\'' +
                '}';
    }
}