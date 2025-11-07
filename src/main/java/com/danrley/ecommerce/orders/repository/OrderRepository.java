package com.danrley.ecommerce.orders.repository;

import com.danrley.ecommerce.orders.entity.Order;
import com.danrley.ecommerce.shared.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para operações de acesso a dados da entidade Order.
 *
 * Queries especiais:
 * - findExpiredReservations(): Para job de expiração de reservas (ADR-003)
 * - findByUserId(): Histórico de pedidos do usuário
 * - findByStatus(): Listar pedidos por status
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Busca todos os pedidos de um usuário específico.
     * Ordenado por data de criação (mais recente primeiro).
     *
     * @param userId ID do usuário
     * @return Lista de pedidos do usuário
     */
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    /**
     * Busca pedidos por status.
     *
     * @param status Status do pedido
     * @return Lista de pedidos com o status especificado
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Busca pedidos com reserva de estoque expirada.
     *
     * Critérios:
     * - Status = PENDENTE
     * - reservedUntil < NOW
     *
     * Usado pelo job agendado que:
     * 1. Busca pedidos expirados
     * 2. Devolve estoque reservado (products.reserved_quantity)
     * 3. Muda status para EXPIRED
     *
     * ADR-003: Locks Pessimistas + Reserva Temporária de Estoque
     *
     * @return Lista de pedidos com reserva expirada
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDENTE' AND o.reservedUntil < :now")
    List<Order> findExpiredReservations(@Param("now") LocalDateTime now);

    /**
     * Conta quantos pedidos um usuário tem por status.
     *
     * @param userId ID do usuário
     * @param status Status do pedido
     * @return Quantidade de pedidos
     */
    Long countByUserIdAndStatus(Long userId, OrderStatus status);

    /**
     * Busca pedidos criados em um período específico.
     *
     * @param startDate Data inicial
     * @param endDate Data final
     * @return Lista de pedidos no período
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findByOrderDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Busca pedidos aprovados de um usuário (histórico de compras).
     *
     * @param userId ID do usuário
     * @return Lista de pedidos aprovados
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = 'APROVADO' ORDER BY o.paymentDate DESC")
    List<Order> findApprovedOrdersByUserId(@Param("userId") Long userId);
}