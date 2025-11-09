package com.danrley.ecommerce.orders.mapper;

import com.danrley.ecommerce.orders.entity.Order;
import com.danrley.ecommerce.orders.entity.OrderItem;
import com.danrley.ecommerce.orders.dto.OrderItemResponse;
import com.danrley.ecommerce.orders.dto.OrderResponse;
import com.danrley.ecommerce.orders.entity.Order;
import com.danrley.ecommerce.orders.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper manual para conversão entre entidades e DTOs de pedidos.
 *
 * <p><strong>Por que não usar MapStruct?</strong></p>
 * <p>Para manter simplicidade e controle total sobre as conversões,
 * especialmente em campos calculados (como productName).</p>
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ul>
 *   <li>Converter Order → OrderResponse</li>
 *   <li>Converter OrderItem → OrderItemResponse</li>
 *   <li>Incluir nome do produto (join manual)</li>
 * </ul>
 *
 * @see com.danrley.ecommerce.orders.entity.Order
 * @see com.danrley.ecommerce.orders.dto.OrderResponse
 */
@Component
public class OrderMapper {

    /**
     * Converte entidade Order para DTO OrderResponse.
     *
     * <p>Inclui conversão de todos os itens do pedido.</p>
     *
     * @param order Entidade Order
     * @return DTO OrderResponse
     */
    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .paymentDate(order.getPaymentDate())
                .reservedUntil(order.getReservedUntil())
                .items(toItemResponseList(order.getItems()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Converte lista de Orders para lista de DTOs.
     *
     * @param orders Lista de entidades Order
     * @return Lista de DTOs OrderResponse
     */
    public List<OrderResponse> toResponseList(List<Order> orders) {
        if (orders == null) {
            return List.of();
        }

        return orders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Converte entidade OrderItem para DTO OrderItemResponse.
     *
     * <p>Inclui nome do produto através do relacionamento
     * OrderItem → Product.</p>
     *
     * @param item Entidade OrderItem
     * @return DTO OrderItemResponse
     */
    public OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct() != null ? item.getProduct().getName() : "Produto não encontrado")
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }

    /**
     * Converte lista de OrderItems para lista de DTOs.
     *
     * @param items Lista de entidades OrderItem
     * @return Lista de DTOs OrderItemResponse
     */
    private List<OrderItemResponse> toItemResponseList(List<OrderItem> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }
}