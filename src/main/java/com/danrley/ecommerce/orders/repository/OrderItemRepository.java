package com.danrley.ecommerce.orders.repository;

import com.danrley.ecommerce.orders.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para operações de acesso a dados da entidade OrderItem.
 *
 * Queries especiais:
 * - findByOrderId(): Itens de um pedido específico
 * - findByProductId(): Histórico de vendas de um produto
 *
 * @author Danrley Brasil dos Santos
 * @since 1.0
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Busca todos os itens de um pedido específico.
     * O nome do método é alterado para corresponder ao caminho do objeto (order.id).
     *
     * @param orderId ID do pedido
     * @return Lista de itens do pedido
     */
    List<OrderItem> findByOrder_Id(Long orderId);

    /**
     * Busca histórico de vendas de um produto específico.
     * O nome do método é alterado para corresponder ao caminho do objeto (product.id).
     *
     * @param productId ID do produto
     * @return Lista de itens contendo o produto
     */
    List<OrderItem> findByProduct_Id(Long productId);

    /**
     * Busca itens de pedidos aprovados de um produto (vendas efetivas).
     * A query foi alterada para usar 'oi.product.id' em vez de 'oi.productId'.
     *
     * @param productId ID do produto
     * @return Lista de itens vendidos (apenas pedidos APROVADO)
     */
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE oi.product.id = :productId AND o.status = 'APROVADO'")
    List<OrderItem> findSoldItemsByProductId(@Param("productId") Long productId);

    /**
     * Calcula total de unidades vendidas de um produto.
     * A query foi alterada para usar 'oi.product.id' em vez de 'oi.productId'.
     *
     * @param productId ID do produto
     * @return Soma das quantidades vendidas
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN oi.order o WHERE oi.product.id = :productId AND o.status = 'APROVADO'")
    Long countSoldUnitsByProductId(@Param("productId") Long productId);
}