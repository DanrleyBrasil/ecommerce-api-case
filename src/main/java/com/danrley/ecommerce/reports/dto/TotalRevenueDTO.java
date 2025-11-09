package com.danrley.ecommerce.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para representar o faturamento total em um período específico.
 *
 * <p>Agrega dados de receita e quantidade de pedidos aprovados em um intervalo
 * de datas, permitindo análise de desempenho comercial.</p>
 *
 * <p><strong>Caso de Uso:</strong></p>
 * <ul>
 *   <li>Relatórios gerenciais de faturamento mensal/trimestral</li>
 *   <li>Análise de sazonalidade de vendas</li>
 *   <li>Comparação de performance entre períodos (YoY, MoM)</li>
 *   <li>Cálculo de KPIs financeiros (receita média diária, etc)</li>
 *   <li>Fechamento contábil e fiscal</li>
 * </ul>
 *
 * <p><strong>Observação:</strong> Considera apenas pedidos com status APROVADO.</p>
 *
 * @author Danrley Brasil
 * @version 1.0
 * @since 2025-11-09
 */
public class TotalRevenueDTO {

    /**
     * Data de início do período analisado (inclusive).
     */
    private LocalDate startDate;

    /**
     * Data de fim do período analisado (inclusive).
     */
    private LocalDate endDate;

    /**
     * Valor total faturado no período.
     * Soma de order.total_amount de todos os pedidos APROVADO no intervalo.
     */
    private BigDecimal totalRevenue;

    /**
     * Quantidade total de pedidos aprovados no período.
     */
    private Long orderCount;

    /**
     * Construtor padrão.
     */
    public TotalRevenueDTO() {
    }

    /**
     * Construtor completo.
     *
     * @param startDate data de início do período
     * @param endDate data de fim do período
     * @param totalRevenue receita total
     * @param orderCount quantidade de pedidos
     */
    public TotalRevenueDTO(LocalDate startDate, LocalDate endDate, BigDecimal totalRevenue, Long orderCount) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalRevenue = totalRevenue;
        this.orderCount = orderCount;
    }

    /**
     * Construtor para projeção SQL (sem datas).
     * As datas serão setadas manualmente no Service.
     *
     * @param totalRevenue receita total
     * @param orderCount quantidade de pedidos
     */
    public TotalRevenueDTO(BigDecimal totalRevenue, Long orderCount) {
        this.totalRevenue = totalRevenue;
        this.orderCount = orderCount;
    }

    // Getters e Setters

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    @Override
    public String toString() {
        return "TotalRevenueDTO{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalRevenue=" + totalRevenue +
                ", orderCount=" + orderCount +
                '}';
    }
}