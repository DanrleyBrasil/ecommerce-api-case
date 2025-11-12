package com.danrley.ecommerce.orders.entity;

import com.danrley.ecommerce.auth.entity.User;
import com.danrley.ecommerce.shared.entity.BaseEntity;
import com.danrley.ecommerce.shared.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um pedido no sistema de e-commerce.
 *
 * Features principais:
 * - Suporte a reserva temporária de estoque (ADR-003)
 * - Controle de status do pedido (PENDENTE, APROVADO, CANCELADO, EXPIRED)
 * - Relacionamento cascata com OrderItem
 * - Auditoria automática via BaseEntity
 *
 * Fluxo de vida:
 * 1. Criado com status PENDENTE + reservedUntil = NOW + 10min
 * 2. Pagamento processado → status APROVADO + paymentDate preenchida
 * 3. Reserva expira → Job altera para EXPIRED
 * 4. Pode ser cancelado → status CANCELADO
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 * @see OrderItem
 * @see OrderStatus
 * @see com.danrley.ecommerce.orders.repository.OrderRepository#findExpiredReservations()
 */
@Data
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_date", columnList = "order_date"),
        @Index(name = "idx_orders_reserved_until", columnList = "reserved_until")
})
public class Order extends BaseEntity {

    /**
     * ID do usuário que fez o pedido.
     * Relacionamento Many-to-One com User.
     */
    @NotNull(message = "Usuário é obrigatório")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * Status atual do pedido.
     * Valores possíveis: PENDENTE, APROVADO, CANCELADO, EXPIRED
     */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    /**
     * Valor total do pedido (soma dos subtotais dos itens).
     * Calculado automaticamente no momento da criação.
     */
    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true, message = "Valor total não pode ser negativo")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Data/hora em que o pedido foi criado.
     * Preenchida automaticamente no momento da criação.
     */
    @NotNull
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    /**
     * Data/hora em que o pagamento foi processado.
     * NULL enquanto status = PENDENTE.
     * Preenchida quando status muda para APROVADO.
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * Data/hora limite da reserva temporária de estoque (ADR-003).
     *
     * Funcionalidade:
     * - Ao criar pedido: reservedUntil = NOW + 10 minutos
     * - Job periódico verifica pedidos com reservedUntil < NOW
     * - Se expirado: status → EXPIRED, estoque devolvido
     *
     * NULL quando:
     * - Pedido aprovado (reserva foi convertida em baixa definitiva)
     * - Pedido cancelado
     * - Pedido expirado
     */
    @Column(name = "reserved_until")
    private LocalDateTime reservedUntil;

    /**
     * Itens do pedido.
     * Relacionamento One-to-Many com cascade ALL.
     *
     * Cascade:
     * - PERSIST: Ao salvar Order, salva OrderItems automaticamente
     * - MERGE: Ao atualizar Order, atualiza OrderItems
     * - REMOVE: Ao deletar Order, deleta OrderItems (orphanRemoval)
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // CONSTRUTORES

    public Order() {
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDENTE;
    }

    // MÉTODOS DE NEGÓCIO

    /**
     * Calcula o valor total do pedido somando os subtotais dos itens.
     *
     * @return Valor total do pedido
     */
    public BigDecimal calculateTotal() {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Adiciona um item ao pedido.
     * Configura o relacionamento bidirecional automaticamente.
     *
     * @param item Item a ser adicionado
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Remove um item do pedido.
     *
     * @param item Item a ser removido
     */
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    /**
     * Verifica se o pedido está pendente.
     *
     * @return true se status = PENDENTE
     */
    public boolean isPending() {
        return OrderStatus.PENDENTE.equals(this.status);
    }

    /**
     * Verifica se o pedido foi aprovado.
     *
     * @return true se status = APROVADO
     */
    public boolean isApproved() {
        return OrderStatus.APROVADO.equals(this.status);
    }

    /**
     * Verifica se a reserva de estoque expirou.
     *
     * @return true se reservedUntil < NOW
     */
    public boolean isReservationExpired() {
        return reservedUntil != null && reservedUntil.isBefore(LocalDateTime.now());
    }

    /**
     * Aprova o pedido, marcando como APROVADO e registrando data de pagamento.
     * Limpa o campo reservedUntil (reserva foi convertida em baixa definitiva).
     */
    public void approve() {
        this.status = OrderStatus.APROVADO;
        this.paymentDate = LocalDateTime.now();
        this.reservedUntil = null; // Reserva convertida em baixa definitiva
    }

    /**
     * Cancela o pedido, marcando como CANCELADO.
     * Limpa o campo reservedUntil.
     */
    public void cancel() {
        this.status = OrderStatus.CANCELADO;
        this.reservedUntil = null;
    }

    /**
     * Marca o pedido como expirado (reserva de estoque venceu).
     * Limpa o campo reservedUntil.
     */
    public void expire() {
        this.status = OrderStatus.EXPIRED;
        this.reservedUntil = null;
    }

    /**
     * Define a reserva de estoque para um período específico.
     * Usado ao criar pedidos (TTL de 10 minutos).
     *
     * @param minutes Minutos de reserva (padrão: 10)
     */
    public void setReservationTTL(int minutes) {
        this.reservedUntil = LocalDateTime.now().plusMinutes(minutes);
    }



    // EQUALS, HASHCODE, TOSTRING

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return getId() != null && getId().equals(order.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + getId() +
                ", userId=" + userId +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", orderDate=" + orderDate +
                ", reservedUntil=" + reservedUntil +
                '}';
    }
}