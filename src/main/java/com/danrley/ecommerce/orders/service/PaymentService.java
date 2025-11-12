package com.danrley.ecommerce.orders.service;

import com.danrley.ecommerce.orders.entity.Order;
import com.danrley.ecommerce.orders.entity.OrderItem;
import com.danrley.ecommerce.orders.repository.OrderRepository;
import com.danrley.ecommerce.products.entity.Product;
import com.danrley.ecommerce.products.repository.ProductRepository;
import com.danrley.ecommerce.products.service.ProductService;
import com.danrley.ecommerce.shared.enums.OrderStatus;
import com.danrley.ecommerce.shared.exception.InsufficientStockException;
import com.danrley.ecommerce.shared.exception.InvalidOrderStatusException;
import com.danrley.ecommerce.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service responsável pelo processamento de pagamentos.
 *
 * <p><strong>Estratégia de Locks Pessimistas (ADR-003):</strong></p>
 * <p>Durante o processamento de pagamento, aplicamos lock pessimista
 * (SELECT FOR UPDATE) nos produtos para garantir consistência em
 * cenários de alta concorrência.</p>
 *
 * <p><strong>Por que separar em um Service próprio?</strong></p>
 * <ul>
 *   <li><strong>Coesão:</strong> OrderService foca em CRUD, PaymentService em processamento</li>
 *   <li><strong>Evolução:</strong> Facilita integração futura com gateways (Stripe, PagSeguro)</li>
 *   <li><strong>Testabilidade:</strong> Isolar lógica complexa de pagamento</li>
 * </ul>
 *
 * <p><strong>Fluxo de Processamento:</strong></p>
 * <ol>
 *   <li>Buscar Order (validar status = PENDENTE)</li>
 *   <li>Validar se reserva não expirou</li>
 *   <li><strong>Aplicar lock pessimista</strong> em todos os produtos</li>
 *   <li>Re-validar estoque (pode ter sido vendido entre criação e pagamento)</li>
 *   <li>Baixar estoque definitivamente (stock_quantity -= quantity)</li>
 *   <li>Liberar reserva (reserved_quantity -= quantity)</li>
 *   <li>Atualizar Order: status → APROVADO, paymentDate = NOW</li>
 * </ol>
 *
 * <p><strong>Isolamento de Transação:</strong></p>
 * <p>Usamos {@code Isolation.SERIALIZABLE} para máxima consistência
 * em operações de estoque críticas.</p>
 *
 * @see com.danrley.ecommerce.orders.service.OrderService
 * @see com.danrley.ecommerce.products.repository.ProductRepository#findByIdWithLock
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    /**
     * Processa pagamento de um pedido com locks pessimistas.
     *
     * <p><strong>Cenário de Concorrência (por que precisamos de locks):</strong></p>
     * <pre>
     * Estoque: 5 unidades
     *
     * Cliente A: cria pedido com 3 unidades (status PENDENTE, reserva: 3)
     * Cliente B: cria pedido com 2 unidades (status PENDENTE, reserva: 2)
     *
     * Estoque disponível: 5 - 5(reservado) = 0
     *
     * Cliente A paga primeiro:
     *   - Lock pessimista garante exclusividade
     *   - stock_quantity: 5 → 2
     *   - reserved_quantity: 5 → 2
     *   - Status → APROVADO
     *
     * Cliente B paga depois:
     *   - Lock pessimista garante leitura consistente
     *   - stock_quantity: 2 (atualizado)
     *   - reserved_quantity: 2
     *   - Disponível: 2 - 2 = 0 ✅ OK!
     *   - stock_quantity: 2 → 0
     *   - reserved_quantity: 2 → 0
     *   - Status → APROVADO
     * </pre>
     *
     * <p><strong>Validações:</strong></p>
     * <ul>
     *   <li>Order deve existir</li>
     *   <li>Status deve ser PENDENTE</li>
     *   <li>Reserva não deve ter expirado</li>
     *   <li>Estoque deve ser suficiente (re-validação)</li>
     * </ul>
     *
     * <p><strong>Exceções:</strong></p>
     * <ul>
     *   <li>{@link ResourceNotFoundException} - Pedido não encontrado</li>
     *   <li>{@link InvalidOrderStatusException} - Status inválido ou expirado</li>
     *   <li>{@link InsufficientStockException} - Estoque insuficiente na re-validação</li>
     * </ul>
     *
     * @param orderId ID do pedido a ser processado
     * @throws ResourceNotFoundException se pedido não existir
     * @throws InvalidOrderStatusException se status inválido ou expirado
     * @throws InsufficientStockException se estoque insuficiente
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processPayment(Long orderId) {
        log.info("Processando pagamento do pedido orderId={}", orderId);

        // 1. Buscar Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // 2. Validar status
        if (order.getStatus() != OrderStatus.PENDENTE) {
            throw InvalidOrderStatusException.alreadyProcessed(orderId, order.getStatus());
        }

        // 3. Validar se reserva não expirou
        if (order.isReservationExpired()) {
            log.warn("Tentativa de pagamento de pedido expirado: orderId={}, reservedUntil={}",
                    orderId, order.getReservedUntil());
            throw InvalidOrderStatusException.expired(orderId, order.getReservedUntil());
        }

        // 4. Processar cada item com LOCK PESSIMISTA
        for (OrderItem item : order.getItems()) {
            processOrderItem(item);
        }

        // 5. Aprovar pedido
        order.approve();
        order.setPaymentDate(LocalDateTime.now());
        order.setReservedUntil(null); // Limpar TTL

        orderRepository.save(order);

        log.info("Pagamento processado com sucesso: orderId={}, totalAmount={}",
                orderId, order.getTotalAmount());
    }

    /**
     * Processa item individual do pedido com lock pessimista.
     *
     * <p><strong>Lock Pessimista (SELECT FOR UPDATE):</strong></p>
     * <p>A lógica de lock é DELEGADA para o ProductService, que é o
     * guardião da consistência do estoque.</p>
     *
     * <p><strong>Operações Atômicas (no ProductService):</strong></p>
     * <ol>
     *   <li>Buscar produto com lock</li>
     *   <li>Re-validar estoque</li>
     *   <li>Baixar estoque (stock_quantity)</li>
     *   <li>Liberar reserva (reserved_quantity)</li>
     * </ol>
     *
     * @param item Item a ser processado
     * @throws ResourceNotFoundException se produto não existir (lançado pelo ProductService)
     * @throws InsufficientStockException se estoque insuficiente (lançado pelo ProductService)
     */
    private void processOrderItem(OrderItem item) {
        // Delega toda a lógica de lock, validação e baixa de estoque para o ProductService.
        // O PaymentService apenas orquestra a chamada.
        productService.finalizeStockDebit(item.getProduct().getId(), item.getQuantity());
    }
}