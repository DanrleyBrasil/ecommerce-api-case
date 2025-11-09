package com.danrley.ecommerce.reports.service;

import com.danrley.ecommerce.reports.dto.AverageTicketDTO;
import com.danrley.ecommerce.reports.dto.TopBuyerDTO;
import com.danrley.ecommerce.reports.dto.TotalRevenueDTO;
import com.danrley.ecommerce.reports.repository.ReportRepositoryCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsável pela lógica de negócio dos relatórios gerenciais.
 *
 * <p>Coordena a execução de queries otimizadas e transforma resultados brutos
 * (Object[]) em DTOs tipados para consumo pela camada de apresentação.</p>
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ul>
 *   <li>Orquestrar chamadas ao ReportRepository</li>
 *   <li>Transformar Object[] em DTOs tipados</li>
 *   <li>Aplicar regras de negócio e validações</li>
 *   <li>Logar execuções para auditoria de performance</li>
 * </ul>
 *
 * <p><strong>Estratégia Transacional:</strong></p>
 * <ul>
 *   <li>@Transactional(readOnly=true) para otimizar queries de leitura</li>
 *   <li>Reduz overhead de flush do Hibernate</li>
 *   <li>Permite otimizações do driver JDBC</li>
 * </ul>
 *
 * @author Danrley Brasil
 * @version 1.0
 * @since 2025-11-09
 */
@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepositoryCustom reportRepositoryCustom;

    /**
     * Construtor com injeção de dependências.
     *
     * @param reportRepositoryCustom repository de relatórios
     */
    public ReportService(ReportRepositoryCustom reportRepositoryCustom) {
        this.reportRepositoryCustom = reportRepositoryCustom;
    }

    /**
     * Retorna os TOP 5 usuários que mais compraram no sistema.
     *
     * <p><strong>Regras de Negócio:</strong></p>
     * <ul>
     *   <li>Considera apenas pedidos com status APROVADO</li>
     *   <li>Ordenação: valor total gasto (DESC) → quantidade de pedidos (DESC)</li>
     *   <li>Retorna no máximo 5 resultados</li>
     * </ul>
     *
     * <p><strong>Performance:</strong></p>
     * <ul>
     *   <li>Tempo esperado: ~50ms para 1000 pedidos</li>
     *   <li>Usa índices: idx_orders_user_id, idx_orders_status</li>
     * </ul>
     *
     * @return lista com os 5 maiores compradores (pode ser vazia se sem pedidos)
     */
    @Transactional(readOnly = true)
    public List<TopBuyerDTO> getTopBuyers() {
        logger.info("Executando relatório: Top 5 compradores");

        long startTime = System.currentTimeMillis();
        List<Object[]> results = reportRepositoryCustom.findTopBuyers();
        long executionTime = System.currentTimeMillis() - startTime;

        logger.info("Query executada em {}ms, {} resultados encontrados", executionTime, results.size());

        return results.stream()
                .map(this::mapToTopBuyerDTO)
                .collect(Collectors.toList());
    }

    /**
     * Calcula o ticket médio de compra de cada usuário.
     *
     * <p><strong>Fórmula:</strong></p>
     * <pre>
     * Ticket Médio = SUM(total_amount) / COUNT(pedidos aprovados)
     * </pre>
     *
     * <p><strong>Regras de Negócio:</strong></p>
     * <ul>
     *   <li>Inclui apenas usuários com pelo menos 1 pedido aprovado</li>
     *   <li>Ordenação: ticket médio decrescente</li>
     *   <li>Valores arredondados para 2 casas decimais</li>
     * </ul>
     *
     * <p><strong>Performance:</strong></p>
     * <ul>
     *   <li>Tempo esperado: ~60ms para 1000 pedidos</li>
     *   <li>Usa índices: idx_orders_user_id, idx_orders_status</li>
     * </ul>
     *
     * @return lista com ticket médio por usuário (pode ser vazia)
     */
    @Transactional(readOnly = true)
    public List<AverageTicketDTO> getAverageTicketByUser() {
        logger.info("Executando relatório: Ticket médio por usuário");

        long startTime = System.currentTimeMillis();
        List<Object[]> results = reportRepositoryCustom.findAverageTicketByUser();
        long executionTime = System.currentTimeMillis() - startTime;

        logger.info("Query executada em {}ms, {} resultados encontrados", executionTime, results.size());

        return results.stream()
                .map(this::mapToAverageTicketDTO)
                .collect(Collectors.toList());
    }

    /**
     * Calcula o faturamento total em um período específico.
     *
     * <p><strong>Regras de Negócio:</strong></p>
     * <ul>
     *   <li>Considera apenas pedidos com status APROVADO</li>
     *   <li>Datas são inclusivas (startDate e endDate fazem parte do período)</li>
     *   <li>Se não houver pedidos no período, retorna receita 0.00 e count 0</li>
     * </ul>
     *
     * <p><strong>Validações:</strong></p>
     * <ul>
     *   <li>startDate não pode ser null</li>
     *   <li>endDate não pode ser null</li>
     *   <li>startDate não pode ser posterior a endDate</li>
     * </ul>
     *
     * <p><strong>Performance:</strong></p>
     * <ul>
     *   <li>Tempo esperado: ~30ms para período de 30 dias</li>
     *   <li>Usa índices: idx_orders_status, idx_orders_date</li>
     * </ul>
     *
     * @param startDate data de início do período (obrigatória)
     * @param endDate data de fim do período (obrigatória)
     * @return DTO com receita total e quantidade de pedidos
     * @throws IllegalArgumentException se datas forem inválidas
     */
    @Transactional(readOnly = true)
    public TotalRevenueDTO getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        logger.info("Executando relatório: Faturamento total de {} até {}", startDate, endDate);

        // Validações de entrada
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Datas de início e fim são obrigatórias");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Data de início não pode ser posterior à data de fim");
        }

        long startTime = System.currentTimeMillis();
        Object[] result = reportRepositoryCustom.findTotalRevenueByPeriod(startDate, endDate);
        long executionTime = System.currentTimeMillis() - startTime;

        logger.info("Query executada em {}ms", executionTime);

        return mapToTotalRevenueDTO(result, startDate, endDate);
    }

    /**
     * Mapeia Object[] para TopBuyerDTO.
     *
     * <p><strong>Estrutura esperada do array:</strong></p>
     * <ul>
     *   <li>[0] userId (Long ou BigInteger)</li>
     *   <li>[1] userName (String)</li>
     *   <li>[2] totalOrders (Long ou BigInteger)</li>
     *   <li>[3] totalSpent (BigDecimal)</li>
     * </ul>
     *
     * @param row linha retornada pela query SQL
     * @return DTO tipado
     */
    private TopBuyerDTO mapToTopBuyerDTO(Object[] row) {
        Long userId = ((Number) row[0]).longValue();
        String userName = (String) row[1];
        Long totalOrders = ((Number) row[2]).longValue();
        BigDecimal totalSpent = (BigDecimal) row[3];

        return new TopBuyerDTO(userId, userName, totalOrders, totalSpent);
    }

    /**
     * Mapeia Object[] para AverageTicketDTO.
     *
     * <p><strong>Estrutura esperada do array:</strong></p>
     * <ul>
     *   <li>[0] userId (Long ou BigInteger)</li>
     *   <li>[1] userName (String)</li>
     *   <li>[2] averageTicket (BigDecimal)</li>
     * </ul>
     *
     * @param row linha retornada pela query SQL
     * @return DTO tipado
     */
    private AverageTicketDTO mapToAverageTicketDTO(Object[] row) {
        Long userId = ((Number) row[0]).longValue();
        String userName = (String) row[1];
        BigDecimal averageTicket = (BigDecimal) row[2];

        return new AverageTicketDTO(userId, userName, averageTicket);
    }

    /**
     * Mapeia Object[] para TotalRevenueDTO.
     *
     * <p><strong>Estrutura esperada do array:</strong></p>
     * <ul>
     *   <li>[0] totalRevenue (BigDecimal)</li>
     *   <li>[1] orderCount (Long ou BigInteger)</li>
     * </ul>
     *
     * @param row linha retornada pela query SQL
     * @param startDate data de início (para incluir no DTO)
     * @param endDate data de fim (para incluir no DTO)
     * @return DTO tipado
     */
    private TotalRevenueDTO mapToTotalRevenueDTO(Object[] row, LocalDate startDate, LocalDate endDate) {
        BigDecimal totalRevenue = (BigDecimal) row[0];
        Long orderCount = ((Number) row[1]).longValue();

        return new TotalRevenueDTO(startDate, endDate, totalRevenue, orderCount);
    }
}