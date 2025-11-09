package com.danrley.ecommerce.orders.controller;

import com.danrley.ecommerce.auth.security.JwtService;
import com.danrley.ecommerce.orders.dto.CreateOrderRequest;
import com.danrley.ecommerce.orders.dto.OrderResponse;
import com.danrley.ecommerce.orders.service.OrderService;
import com.danrley.ecommerce.orders.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para gerenciamento de pedidos.
 *
 * <p><strong>Endpoints Disponíveis:</strong></p>
 * <ul>
 *   <li>POST /api/orders - Criar novo pedido (USER)</li>
 *   <li>GET /api/orders - Listar pedidos (USER vê seus, ADMIN vê todos)</li>
 *   <li>GET /api/orders/{id} - Buscar pedido por ID (USER vê seu, ADMIN vê qualquer)</li>
 *   <li>POST /api/orders/{id}/payment - Processar pagamento (USER para seu pedido, ADMIN para qualquer)</li>
 *   <li>DELETE /api/orders/{id} - Cancelar pedido (ADMIN only)</li>
 * </ul>
 *
 * <p><strong>Autenticação:</strong></p>
 * <p>Todos os endpoints requerem JWT válido no header:
 * {@code Authorization: Bearer <token>}</p>
 *
 * <p><strong>Permissões:</strong></p>
 * <ul>
 *   <li><strong>USER:</strong> Pode criar e visualizar SEUS pedidos</li>
 *   <li><strong>ADMIN:</strong> Pode visualizar TODOS os pedidos e cancelar qualquer um</li>
 * </ul>
 *
 * <p><strong>Fluxo Completo (ADR-003):</strong></p>
 * <ol>
 *   <li>USER cria pedido → Estoque reservado temporariamente (10min TTL)</li>
 *   <li>USER processa pagamento → Lock pessimista + baixa definitiva</li>
 *   <li>Se não pagar em 10min → Job expira automaticamente</li>
 * </ol>
 *
 * @see com.danrley.ecommerce.orders.service.OrderService
 * @see com.danrley.ecommerce.orders.service.PaymentService
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gerenciamento de Pedidos")
@SecurityRequirement(name = "bearer-jwt")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final JwtService jwtService;  // ✅ ADICIONAR

    /**
     * Cria um novo pedido com reserva temporária de estoque.
     *
     * <p><strong>Permissão:</strong> USER ou ADMIN</p>
     *
     * <p><strong>Fluxo (ADR-003):</strong></p>
     * <ol>
     *   <li>Validar estoque disponível de todos os produtos</li>
     *   <li>Reservar estoque temporariamente (reserved_quantity)</li>
     *   <li>Criar Order com status PENDENTE</li>
     *   <li>Setar TTL de 10 minutos (reservedUntil)</li>
     *   <li>Retornar Order com informações para pagamento</li>
     * </ol>
     *
     * <p><strong>Exemplo de Requisição:</strong></p>
     * <pre>
     * POST /api/orders
     * Authorization: Bearer eyJhbGc...
     *
     * {
     *   "items": [
     *     {
     *       "productId": 1,
     *       "quantity": 2
     *     },
     *     {
     *       "productId": 5,
     *       "quantity": 1
     *     }
     *   ]
     * }
     * </pre>
     *
     * <p><strong>Respostas:</strong></p>
     * <ul>
     *   <li>201 Created - Pedido criado com sucesso</li>
     *   <li>400 Bad Request - Dados inválidos</li>
     *   <li>404 Not Found - Produto não encontrado</li>
     *   <li>409 Conflict - Estoque insuficiente</li>
     *   <li>401 Unauthorized - Token inválido/ausente</li>
     * </ul>
     *
     * @param request Dados do pedido (lista de itens)
     * @param authentication Dados do usuário autenticado (JWT)
     * @return OrderResponse com dados do pedido criado
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Criar novo pedido",
            description = "Cria um pedido com reserva temporária de estoque (TTL: 10 minutos). " +
                    "O estoque é reservado mas não baixado até o pagamento ser processado.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Pedido criado com sucesso",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
                    @ApiResponse(responseCode = "409", description = "Estoque insuficiente"),
                    @ApiResponse(responseCode = "401", description = "Não autenticado")
            }
    )
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {  // ✅ MUDOU DE Authentication para HttpServletRequest

        Long userId = extractUserId(httpRequest);
        log.info("Criando pedido para userId={}", userId);

        OrderResponse response = orderService.createOrder(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista pedidos do usuário autenticado.
     *
     * <p><strong>Comportamento por Role:</strong></p>
     * <ul>
     *   <li><strong>USER:</strong> Lista APENAS seus pedidos</li>
     *   <li><strong>ADMIN:</strong> Lista TODOS os pedidos do sistema</li>
     * </ul>
     *
     * <p><strong>Ordenação:</strong> Por data de criação (mais recente primeiro)</p>
     *
     * <p><strong>Exemplo de Requisição:</strong></p>
     * <pre>
     * GET /api/orders
     * Authorization: Bearer eyJhbGc...
     * </pre>
     *
     * @param authentication Dados do usuário autenticado
     * @return Lista de OrderResponse
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Listar pedidos",
            description = "USER vê apenas seus pedidos. ADMIN vê todos os pedidos do sistema.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de pedidos retornada com sucesso"
                    ),
                    @ApiResponse(responseCode = "401", description = "Não autenticado")
            }
    )
    public ResponseEntity<List<OrderResponse>> listOrders(
            HttpServletRequest request,  // ✅ MUDOU
            Authentication authentication) {  // ✅ MANTER para checar role

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        List<OrderResponse> orders;

        if (isAdmin) {
            log.info("ADMIN listando TODOS os pedidos");
            orders = orderService.getAllOrders();
        } else {
            Long userId = extractUserId(request);  // ✅ USAR O NOVO MÉTODO
            log.info("USER listando seus pedidos: userId={}", userId);
            orders = orderService.getUserOrders(userId);
        }

        return ResponseEntity.ok(orders);
    }

    /**
     * Busca pedido por ID.
     *
     * <p><strong>Validação de Permissão:</strong></p>
     * <ul>
     *   <li><strong>USER:</strong> Só pode ver SEU pedido (userId do pedido = userId do token)</li>
     *   <li><strong>ADMIN:</strong> Pode ver QUALQUER pedido</li>
     * </ul>
     *
     * <p><strong>Exemplo de Requisição:</strong></p>
     * <pre>
     * GET /api/orders/1
     * Authorization: Bearer eyJhbGc...
     * </pre>
     *
     * @param id ID do pedido
     * @param authentication Dados do usuário autenticado
     * @return OrderResponse
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Buscar pedido por ID",
            description = "USER só pode visualizar seus próprios pedidos. ADMIN pode visualizar qualquer pedido.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Pedido encontrado",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
                    @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este pedido"),
                    @ApiResponse(responseCode = "401", description = "Não autenticado")
            }
    )
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            HttpServletRequest request,  // ✅ ADICIONAR
            Authentication authentication) {

        OrderResponse order = orderService.getOrderById(id);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Long userId = extractUserId(request);  // ✅ USAR O NOVO MÉTODO
            if (!order.getUserId().equals(userId)) {
                log.warn("USER tentou acessar pedido de outro usuário: userId={}, orderId={}", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(order);
    }

    /**
     * Processa pagamento de um pedido com locks pessimistas.
     *
     * <p><strong>Permissão:</strong> USER ou ADMIN</p>
     * <p>USER só pode pagar seu próprio pedido.</p>
     *
     * <p><strong>Fluxo de Pagamento (ADR-003):</strong></p>
     * <ol>
     *   <li>Buscar Order (validar status = PENDENTE)</li>
     *   <li>Validar se reserva não expirou</li>
     *   <li>Aplicar lock pessimista (SELECT FOR UPDATE) nos produtos</li>
     *   <li>Re-validar estoque (pode ter mudado)</li>
     *   <li>Baixar estoque definitivamente (stock_quantity)</li>
     *   <li>Liberar reserva (reserved_quantity)</li>
     *   <li>Status → APROVADO, paymentDate = NOW</li>
     * </ol>
     *
     * <p><strong>Exemplo de Requisição:</strong></p>
     * <pre>
     * POST /api/orders/1/payment
     * Authorization: Bearer eyJhbGc...
     * </pre>
     *
     * <p><strong>Respostas:</strong></p>
     * <ul>
     *   <li>200 OK - Pagamento processado com sucesso</li>
     *   <li>400 Bad Request - Status inválido ou pedido expirado</li>
     *   <li>404 Not Found - Pedido não encontrado</li>
     *   <li>409 Conflict - Estoque insuficiente na re-validação</li>
     *   <li>403 Forbidden - USER tentando pagar pedido de outro usuário</li>
     * </ul>
     *
     * @param id ID do pedido
     * @param authentication Dados do usuário autenticado
     * @return ResponseEntity vazio (204 No Content)
     */
    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Processar pagamento do pedido",
            description = "Aplica lock pessimista nos produtos, baixa estoque definitivamente e aprova o pedido. " +
                    "USER só pode processar pagamento de seus próprios pedidos.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagamento processado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Status inválido ou pedido expirado"),
                    @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
                    @ApiResponse(responseCode = "409", description = "Estoque insuficiente"),
                    @ApiResponse(responseCode = "403", description = "Sem permissão para processar este pagamento"),
                    @ApiResponse(responseCode = "401", description = "Não autenticado")
            }
    )
    public ResponseEntity<Void> processPayment(
            @PathVariable Long id,
            HttpServletRequest request,  // ✅ ADICIONAR
            Authentication authentication) {

        OrderResponse order = orderService.getOrderById(id);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Long userId = extractUserId(request);  // ✅ USAR O NOVO MÉTODO
            if (!order.getUserId().equals(userId)) {
                log.warn("USER tentou processar pagamento de pedido de outro usuário: userId={}, orderId={}", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        log.info("Processando pagamento: orderId={}", id);
        paymentService.processPayment(id);

        return ResponseEntity.ok().build();
    }

    /**
     * Cancela um pedido manualmente.
     *
     * <p><strong>Permissão:</strong> ADMIN ONLY</p>
     *
     * <p><strong>Regras:</strong></p>
     * <ul>
     *   <li>Apenas pedidos PENDENTES podem ser cancelados</li>
     *   <li>Libera reserva de estoque (reserved_quantity)</li>
     *   <li>Status → CANCELADO</li>
     * </ul>
     *
     * <p><strong>Exemplo de Requisição:</strong></p>
     * <pre>
     * DELETE /api/orders/1
     * Authorization: Bearer eyJhbGc... (ADMIN)
     * </pre>
     *
     * @param id ID do pedido
     * @return ResponseEntity vazio (204 No Content)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cancelar pedido (ADMIN)",
            description = "Cancela um pedido PENDENTE e libera a reserva de estoque. Apenas ADMIN pode executar.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Pedido cancelado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Pedido já foi processado"),
                    @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
                    @ApiResponse(responseCode = "403", description = "Apenas ADMIN pode cancelar pedidos"),
                    @ApiResponse(responseCode = "401", description = "Não autenticado")
            }
    )
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "ID do pedido") @PathVariable Long id) {

        log.info("Cancelando pedido (ADMIN): orderId={}", id);
        orderService.cancelOrder(id);

        return ResponseEntity.noContent().build();
    }

    // ========================================
    // MÉTODOS AUXILIARES PRIVADOS
    // ========================================

    /**
     * Extrai userId do claim customizado do JWT.
     *
     * <p>O userId está armazenado como claim customizado no token JWT,
     * definido durante a geração do token no AuthService.</p>
     *
     * <p><strong>Arquitetura:</strong></p>
     * <p>Usar JwtService (módulo AUTH) é aceitável aqui porque:
     * - JWT é infraestrutura transversal (shared concern)
     * - Não acopla lógica de negócio entre módulos
     * - OrderController não conhece User ou UserRepository
     * - Apenas extrai informação já presente no token</p>
     *
     * @param request Request HTTP contendo o token JWT
     * @return ID do usuário autenticado
     */
    private Long extractUserId(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return jwtService.extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extrai token JWT do header Authorization.
     *
     * @param request Request HTTP
     * @return Token JWT (sem o prefixo "Bearer ")
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Token JWT não encontrado no header");
        }

        return authHeader.substring(7); // Remove "Bearer "
    }
}