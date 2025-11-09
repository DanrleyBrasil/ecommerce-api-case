package com.danrley.ecommerce.reports.dto;

import java.math.BigDecimal;

/**
 * DTO para representar o ticket médio de compra por usuário.
 *
 * <p>Calcula a média de valor gasto por pedido para cada cliente,
 * permitindo análise do comportamento de consumo e segmentação de clientes.</p>
 *
 * <p><strong>Fórmula:</strong></p>
 * <pre>
 * Ticket Médio = SUM(total_amount) / COUNT(orders)
 * </pre>
 *
 * <p><strong>Caso de Uso:</strong></p>
 * <ul>
 *   <li>Identificar clientes com alto ticket médio (potencial premium)</li>
 *   <li>Comparar ticket médio entre segmentos de clientes</li>
 *   <li>Avaliar efetividade de estratégias de upselling</li>
 *   <li>Definir metas de vendas por vendedor</li>
 * </ul>
 *
 * @author Danrley Brasil
 * @version 1.0
 * @since 2025-11-09
 */
public class AverageTicketDTO {

    /**
     * Identificador único do usuário.
     */
    private Long userId;

    /**
     * Nome completo do usuário.
     */
    private String userName;

    /**
     * Ticket médio de compra do usuário.
     * Calculado como: SUM(total_amount) / COUNT(pedidos aprovados).
     */
    private BigDecimal averageTicket;

    /**
     * Construtor padrão.
     */
    public AverageTicketDTO() {
    }

    /**
     * Construtor completo usado por projeções JPA/JPQL.
     *
     * @param userId identificador do usuário
     * @param userName nome do usuário
     * @param averageTicket valor médio dos pedidos
     */
    public AverageTicketDTO(Long userId, String userName, BigDecimal averageTicket) {
        this.userId = userId;
        this.userName = userName;
        this.averageTicket = averageTicket;
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

    public BigDecimal getAverageTicket() {
        return averageTicket;
    }

    public void setAverageTicket(BigDecimal averageTicket) {
        this.averageTicket = averageTicket;
    }

    @Override
    public String toString() {
        return "AverageTicketDTO{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", averageTicket=" + averageTicket +
                '}';
    }
}