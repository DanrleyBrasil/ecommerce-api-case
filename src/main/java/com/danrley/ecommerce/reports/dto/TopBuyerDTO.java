package com.danrley.ecommerce.reports.dto;

import java.math.BigDecimal;

/**
 * DTO para representar os maiores compradores do sistema.
 *
 * <p>Contém informações agregadas sobre usuários e seus históricos de compras,
 * incluindo quantidade de pedidos realizados e valor total gasto.</p>
 *
 * <p><strong>Caso de Uso:</strong></p>
 * <ul>
 *   <li>Identificar clientes VIP para programas de fidelidade</li>
 *   <li>Análise de perfil de consumo</li>
 *   <li>Estratégias de marketing direcionado</li>
 * </ul>
 *
 * @author Danrley Brasil
 * @version 1.0
 * @since 2025-11-09
 */
public class TopBuyerDTO {

    /**
     * Identificador único do usuário.
     */
    private Long userId;

    /**
     * Nome completo do usuário.
     */
    private String userName;

    /**
     * Quantidade total de pedidos realizados pelo usuário.
     * Contabiliza apenas pedidos com status APROVADO.
     */
    private Long totalOrders;

    /**
     * Valor total gasto pelo usuário em todos os pedidos aprovados.
     * Soma de order.total_amount de todos os pedidos APROVADO.
     */
    private BigDecimal totalSpent;

    /**
     * Construtor padrão.
     */
    public TopBuyerDTO() {
    }

    /**
     * Construtor completo usado por projeções JPA/JPQL.
     *
     * @param userId identificador do usuário
     * @param userName nome do usuário
     * @param totalOrders quantidade de pedidos aprovados
     * @param totalSpent valor total gasto
     */
    public TopBuyerDTO(Long userId, String userName, Long totalOrders, BigDecimal totalSpent) {
        this.userId = userId;
        this.userName = userName;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
    }

    // Getters e Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    @Override
    public String toString() {
        return "TopBuyerDTO{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", totalOrders=" + totalOrders +
                ", totalSpent=" + totalSpent +
                '}';
    }
}