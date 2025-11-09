package com.danrley.ecommerce.orders.service;


import com.danrley.ecommerce.orders.dto.OrderResponse;
import com.danrley.ecommerce.orders.entity.Order;
import com.danrley.ecommerce.orders.entity.OrderItem;
import com.danrley.ecommerce.orders.repository.OrderRepository;
import com.danrley.ecommerce.products.entity.Product;
import com.danrley.ecommerce.products.repository.ProductRepository;
import com.danrley.ecommerce.shared.enums.OrderStatus;
import com.danrley.ecommerce.shared.exception.InsufficientStockException;
import com.danrley.ecommerce.shared.exception.InvalidOrderStatusException;
import com.danrley.ecommerce.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsável pela lógica de negócio de pedidos.
 *
 * <p><strong>Estratégia de Controle de Estoque (ADR-003):</strong></p>
 * <ol>
 *   <li><strong>Criação do Pedido:</strong>
 *     <ul>
 *       <li>Validar estoque disponível de TODOS os produtos</li>
 *       <li>Reservar estoque temporariamente (reserved_quantity)</li>
 *       <li>Criar Order com status PENDENTE</li>
 *       <li>Setar TTL de 10 minutos (reservedUntil)</li>
 *     </ul>
 *   </li>
 *   <li><strong>Processamento de Pagamento:</strong>
 *     <ul>
 *       <li>Aplicar lock pessimista nos produtos (ver PaymentService)</li>
 *       <li>Re-validar estoque (pode ter sido vendido/expirado)</li>
 *       <li>Baixar estoque definitivamente</li>
 *       <li>Liberar reserva (reserved_quantity)</li>
 *       <li>Status → APROVADO</li>
 *     </ul>
 *   </li>
 *   <li><strong>Expiração Automática:</strong>
 *     <ul>
 *       <li>Job agendado (@Scheduled) processa pedidos expirados</li>
 *       <li>Libera reserva de volta ao estoque</li>
 *       <li>Status → EXPIRED</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><strong>Permissões:</strong></p>
 * <ul>
 *   <li>USER: pode criar e visualizar SEUS pedidos</li>
 *   <li>ADMIN: pode visualizar TODOS os pedidos e cancelar qualquer um</li>
 * </ul>
 *
 * @see PaymentService
 * @see com.danrley.ecommerce.orders.controller.OrderController
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final com.danrley.ecommerce.orders.mapper.OrderMapper orderMapper;

    /**
     * TTL da reserva de estoque em minutos.
     *
     * <p>Após esse período, job agendado expirará o pedido
     * e liberará a reserva automaticamente.</p>
     */
    private static final int RESERVATION_TTL_MINUTES = 10;

    /**
     * Cria um novo pedido com reserva temporária de estoque.
     *
     * <p><strong>Fluxo de Execução (ADR-003):</strong></p>
     * <ol>
     *   <li>Validar que todos os produtos existem e estão ativos</li>
     *   <li>Validar estoque disponível: (stock - reserved) >= quantity</li>
     *   <li>Reservar estoque temporariamente (reserved_quantity += quantity)</li>
     *   <li>Criar Order com status PENDENTE</li>
     *   <li>Criar OrderItems com snapshot de preços</li>
     *   <li>Calcular e setar totalAmount</li>
     *   <li>Setar reservedUntil = NOW + 10 minutos</li>
     * </ol>
     *
     * <p><strong>Validações:</strong></p>
     * <ul>
     *   <li>Produto deve existir</li>
     *   <li>Produto deve estar ativo (active = true)</li>
     *   <li>Estoque disponível deve ser suficiente</li>
     * </ul>
     *
     * <p><strong>Exceções:</strong></p>
     * <ul>
     *   <li>{@link ResourceNotFoundException} - Produto não encontrado</li>
     *   <li>{@link InsufficientStockException} - Estoque insuficiente</li>
     * </ul>
     *
     * <p><strong>Transação:</strong></p>
     * <p>Método transacional para garantir atomicidade:
     * se qualquer validação falhar, NENHUMA reserva é feita.</p>
     *
     * @param request Dados do pedido (lista de itens)
     * @param userId ID do usuário (extraído do token JWT)
     * @return OrderResponse com dados do pedido criado
     * @throws ResourceNotFoundException se algum produto não existir
     * @throws InsufficientStockException se estoque insuficiente
     */
    @Transactional
    public OrderResponse createOrder(com.danrley.ecommerce.orders.dto.CreateOrderRequest request, Long userId) {
        log.info("Criando pedido para userId={} com {} itens", userId, request.getItems().size());

        // 1. Validar e buscar produtos
        List<Product> products = validateAndFetchProducts(request.getItems());

        // 2. Validar estoque disponível de TODOS os produtos ANTES de reservar
        validateStockAvailability(request.getItems(), products);

        // 3. Reservar estoque temporariamente
        reserveStock(request.getItems(), products);

        // 4. Criar entidade Order
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDENTE);
        order.setOrderDate(LocalDateTime.now());
        order.setReservedUntil(LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES));

        // 5. Criar OrderItems com snapshot de preços
        List<OrderItem> orderItems = createOrderItems(request.getItems(), products, order);
        order.setItems(orderItems);

        // 6. Calcular e setar total DIRETAMENTE NO SERVICE
        BigDecimal totalAmount = orderItems.stream()
                .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        // 7. Salvar Order (cascade persiste OrderItems automaticamente)
        Order savedOrder = orderRepository.save(order);

        log.info("Pedido criado com sucesso: orderId={}, totalAmount={}, reservedUntil={}",
                savedOrder.getId(), savedOrder.getTotalAmount(), savedOrder.getReservedUntil());

        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Busca pedido por ID.
     *
     * <p>Validações de permissão devem ser feitas no Controller:
     * USER só pode ver seus próprios pedidos.</p>
     *
     * @param orderId ID do pedido
     * @return OrderResponse
     * @throws ResourceNotFoundException se pedido não existir
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        log.debug("Buscando pedido orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        return orderMapper.toResponse(order);
    }

    /**
     * Lista todos os pedidos de um usuário.
     *
     * <p>Ordenados por data de criação (mais recente primeiro).</p>
     *
     * @param userId ID do usuário
     * @return Lista de OrderResponse
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        log.debug("Listando pedidos do userId={}", userId);

        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);

        return orderMapper.toResponseList(orders);
    }

    /**
     * Lista TODOS os pedidos do sistema (ADMIN only).
     *
     * <p>Ordenados por data de criação (mais recente primeiro).</p>
     *
     * @return Lista de OrderResponse
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.debug("Listando TODOS os pedidos (ADMIN)");

        List<Order> orders = orderRepository.findAll();
        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));

        return orderMapper.toResponseList(orders);
    }

    /**
     * Cancela um pedido manualmente.
     *
     * <p><strong>Regras:</strong></p>
     * <ul>
     *   <li>Apenas pedidos PENDENTES podem ser cancelados</li>
     *   <li>Libera reserva de estoque</li>
     *   <li>Status → CANCELADO</li>
     * </ul>
     *
     * @param orderId ID do pedido
     * @throws ResourceNotFoundException se pedido não existir
     * @throws InvalidOrderStatusException se pedido já foi processado
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        log.info("Cancelando pedido orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Validar status
        if (order.getStatus() != OrderStatus.PENDENTE) {
            throw InvalidOrderStatusException.alreadyProcessed(orderId, order.getStatus());
        }

        // Liberar reservas
        releaseReservations(order);

        // Atualizar status
        order.cancel();
        orderRepository.save(order);

        log.info("Pedido cancelado com sucesso: orderId={}", orderId);
    }

    // ========================================
    // MÉTODOS AUXILIARES PRIVADOS
    // ========================================

    /**
     * Valida e busca produtos do banco.
     *
     * <p>Garante que todos os produtos existem e estão ativos.</p>
     *
     * @param itemRequests Lista de itens solicitados
     * @return Lista de produtos encontrados (mesma ordem)
     * @throws ResourceNotFoundException se algum produto não existir ou estiver inativo
     */
    private List<Product> validateAndFetchProducts(List<com.danrley.ecommerce.orders.dto.OrderItemRequest> itemRequests) {
        List<Product> products = new ArrayList<>();

        for (com.danrley.ecommerce.orders.dto.OrderItemRequest item : itemRequests) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProductId()));

            // Validar se produto está ativo
            if (!product.getActive()) {
                throw new ResourceNotFoundException("Product", "id", item.getProductId());
            }

            products.add(product);
        }

        return products;
    }

    /**
     * Valida estoque disponível de TODOS os produtos.
     *
     * <p><strong>Fórmula:</strong></p>
     * <pre>
     * estoque_disponivel = stock_quantity - reserved_quantity
     * </pre>
     *
     * <p>Se qualquer produto não tiver estoque suficiente,
     * lança exceção ANTES de fazer qualquer reserva.</p>
     *
     * @param itemRequests Lista de itens solicitados
     * @param products Lista de produtos (mesma ordem)
     * @throws InsufficientStockException se estoque insuficiente
     */
    private void validateStockAvailability(List<com.danrley.ecommerce.orders.dto.OrderItemRequest> itemRequests, List<Product> products) {
        for (int i = 0; i < itemRequests.size(); i++) {
            com.danrley.ecommerce.orders.dto.OrderItemRequest item = itemRequests.get(i);
            Product product = products.get(i);

            int availableStock = product.getStockQuantity() - product.getReservedQuantity();

            if (availableStock < item.getQuantity()) {
                log.warn("Estoque insuficiente: productId={}, disponível={}, solicitado={}",
                        product.getId(), availableStock, item.getQuantity());

                throw new InsufficientStockException(
                        product.getId(),
                        product.getName(),
                        item.getQuantity(),
                        availableStock
                );
            }
        }
    }

    /**
     * Reserva estoque temporariamente (ADR-003).
     *
     * <p>Incrementa reserved_quantity de cada produto.
     * Essa reserva será liberada em 3 cenários:</p>
     * <ol>
     *   <li>Pagamento aprovado → converte em baixa definitiva</li>
     *   <li>Cancelamento manual → devolve ao estoque</li>
     *   <li>Expiração (TTL) → job devolve automaticamente</li>
     * </ol>
     *
     * @param itemRequests Lista de itens solicitados
     * @param products Lista de produtos (mesma ordem)
     */
    private void reserveStock(List<com.danrley.ecommerce.orders.dto.OrderItemRequest> itemRequests, List<Product> products) {
        for (int i = 0; i < itemRequests.size(); i++) {
            com.danrley.ecommerce.orders.dto.OrderItemRequest item = itemRequests.get(i);
            Product product = products.get(i);

            int newReservedQuantity = product.getReservedQuantity() + item.getQuantity();
            product.setReservedQuantity(newReservedQuantity);

            productRepository.save(product);

            log.debug("Estoque reservado: productId={}, quantity={}, newReserved={}",
                    product.getId(), item.getQuantity(), newReservedQuantity);
        }
    }

    /**
     * Cria OrderItems com snapshot de preços.
     *
     * <p>Captura o preço atual do produto (unitPrice) para
     * manter histórico preciso, mesmo que o preço mude depois.</p>
     *
     * @param itemRequests Lista de itens solicitados
     * @param products Lista de produtos (mesma ordem)
     * @param order Order pai
     * @return Lista de OrderItems criados
     */
    private List<OrderItem> createOrderItems(List<com.danrley.ecommerce.orders.dto.OrderItemRequest> itemRequests,
                                             List<Product> products,
                                             Order order) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < itemRequests.size(); i++) {
            com.danrley.ecommerce.orders.dto.OrderItemRequest item = itemRequests.get(i);
            Product product = products.get(i);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            // ===== ALTERAÇÃO 1: A linha abaixo foi removida pois é redundante. =====
            // orderItem.setProductId(product.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(product.getPrice()); // Snapshot!
            // subtotal será calculado automaticamente via @PrePersist

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    /**
     * Libera reservas de estoque de um pedido.
     *
     * <p>Usado em:</p>
     * <ul>
     *   <li>Cancelamento manual</li>
     *   <li>Expiração automática (via job)</li>
     * </ul>
     *
     * @param order Pedido a liberar reservas
     */
    private void releaseReservations(Order order) {
        for (OrderItem item : order.getItems()) {
            // ===== ALTERAÇÃO 2: Acessa o produto diretamente do item, sem nova busca no banco. =====
            Product product = item.getProduct();

            // A busca antiga foi removida:
            // Product product = productRepository.findById(item.getProductId())
            //         .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProductId()));

            int newReservedQuantity = product.getReservedQuantity() - item.getQuantity();
            product.setReservedQuantity(Math.max(0, newReservedQuantity)); // Garantir >= 0

            productRepository.save(product);

            log.debug("Reserva liberada: productId={}, quantity={}, newReserved={}",
                    product.getId(), item.getQuantity(), newReservedQuantity);
        }
    }
}