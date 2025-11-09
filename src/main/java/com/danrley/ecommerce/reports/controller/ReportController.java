package com.danrley.ecommerce.reports.controller;

import com.danrley.ecommerce.reports.dto.AverageTicketDTO;
import com.danrley.ecommerce.reports.dto.TopBuyerDTO;
import com.danrley.ecommerce.reports.dto.TotalRevenueDTO;
import com.danrley.ecommerce.reports.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller REST para endpoints de relatórios gerenciais.
 *
 * <p><strong>Restrições de Acesso:</strong></p>
 * <ul>
 *   <li>Todos os endpoints exigem autenticação via JWT</li>
 *   <li>Todos os endpoints exigem role ADMIN</li>
 *   <li>Usuários com role USER receberão HTTP 403 Forbidden</li>
 * </ul>
 *
 * <p><strong>Endpoints Disponíveis:</strong></p>
 * <ul>
 *   <li>GET /api/reports/top-buyers - Top 5 compradores</li>
 *   <li>GET /api/reports/average-ticket - Ticket médio por usuário</li>
 *   <li>GET /api/reports/revenue - Faturamento por período</li>
 * </ul>
 *
 * <p><strong>Segurança:</strong></p>
 * <ul>
 *   <li>@PreAuthorize garante controle de acesso em nível de método</li>
 *   <li>Swagger UI exige token JWT válido para testar endpoints</li>
 * </ul>
 *
 * @author Danrley Brasil
 * @version 1.0
 * @since 2025-11-09
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Relatórios", description = "Endpoints de relatórios gerenciais - Acesso restrito a ADMIN")
@SecurityRequirement(name = "bearerjwt")
public class ReportController {

    private final ReportService reportService;

    /**
     * Construtor com injeção de dependências.
     *
     * @param reportService service de relatórios
     */
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Retorna os TOP 5 usuários que mais compraram.
     *
     * <p><strong>Critérios de Ordenação:</strong></p>
     * <ol>
     *   <li>Valor total gasto (decrescente)</li>
     *   <li>Quantidade de pedidos (decrescente)</li>
     * </ol>
     *
     * <p><strong>Exemplo de Resposta:</strong></p>
     * <pre>
     * [
     *   {
     *     "userId": 2,
     *     "userName": "João Silva",
     *     "totalOrders": 15,
     *     "totalSpent": 4500.00
     *   },
     *   ...
     * ]
     * </pre>
     *
     * @return lista com os 5 maiores compradores
     */
    @GetMapping("/top-buyers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Top 5 compradores",
            description = "Retorna os 5 usuários que mais compraram (por valor total gasto). Apenas ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório gerado com sucesso",
                    content = @Content(schema = @Schema(implementation = TopBuyerDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado - Token JWT ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado - Usuário não possui role ADMIN",
                    content = @Content
            )
    })
    public ResponseEntity<List<TopBuyerDTO>> getTopBuyers() {
        List<TopBuyerDTO> topBuyers = reportService.getTopBuyers();
        return ResponseEntity.ok(topBuyers);
    }

    /**
     * Retorna o ticket médio de cada usuário.
     *
     * <p><strong>Cálculo:</strong> Ticket Médio = Valor Total Gasto / Quantidade de Pedidos</p>
     *
     * <p><strong>Exemplo de Resposta:</strong></p>
     * <pre>
     * [
     *   {
     *     "userId": 2,
     *     "userName": "João Silva",
     *     "averageTicket": 300.00
     *   },
     *   ...
     * ]
     * </pre>
     *
     * @return lista com ticket médio por usuário (ordenada decrescente)
     */
    @GetMapping("/average-ticket")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Ticket médio por usuário",
            description = "Calcula o ticket médio de compra de cada usuário (SUM/COUNT). Apenas ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório gerado com sucesso",
                    content = @Content(schema = @Schema(implementation = AverageTicketDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content
            )
    })
    public ResponseEntity<List<AverageTicketDTO>> getAverageTicket() {
        List<AverageTicketDTO> averageTickets = reportService.getAverageTicketByUser();
        return ResponseEntity.ok(averageTickets);
    }

    /**
     * Calcula o faturamento total em um período específico.
     *
     * <p><strong>Parâmetros Obrigatórios:</strong></p>
     * <ul>
     *   <li>startDate: data de início (formato: yyyy-MM-dd)</li>
     *   <li>endDate: data de fim (formato: yyyy-MM-dd)</li>
     * </ul>
     *
     * <p><strong>Validações:</strong></p>
     * <ul>
     *   <li>startDate não pode ser posterior a endDate</li>
     *   <li>Ambas as datas são inclusivas</li>
     * </ul>
     *
     * <p><strong>Exemplos de URL:</strong></p>
     * <pre>
     * GET /api/reports/revenue?startDate=2025-01-01&endDate=2025-01-31  // Janeiro/2025
     * GET /api/reports/revenue?startDate=2025-10-01&endDate=2025-12-31  // Q4 2025
     * </pre>
     *
     * <p><strong>Exemplo de Resposta:</strong></p>
     * <pre>
     * {
     *   "startDate": "2025-01-01",
     *   "endDate": "2025-01-31",
     *   "totalRevenue": 45600.00,
     *   "orderCount": 152
     * }
     * </pre>
     *
     * @param startDate data de início do período (obrigatória)
     * @param endDate data de fim do período (obrigatória)
     * @return DTO com receita total e quantidade de pedidos
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Faturamento por período",
            description = "Calcula a receita total e quantidade de pedidos aprovados em um intervalo de datas. Apenas ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório gerado com sucesso",
                    content = @Content(schema = @Schema(implementation = TotalRevenueDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros inválidos (datas ausentes ou startDate > endDate)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado",
                    content = @Content
            )
    })
    public ResponseEntity<TotalRevenueDTO> getTotalRevenue(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", required = true, example = "2025-01-01")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", required = true, example = "2025-01-31")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        TotalRevenueDTO revenue = reportService.getTotalRevenue(startDate, endDate);
        return ResponseEntity.ok(revenue);
    }
}