package com.danrley.ecommerce.reports.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class ReportRepositoryImpl implements ReportRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Object[]> findTopBuyers() {
        String sql = """
                SELECT
                    u.id AS userId,
                    u.name AS userName,
                    COUNT(o.id) AS totalOrders,
                    COALESCE(SUM(o.total_amount), 0) AS totalSpent
                FROM users u
                INNER JOIN orders o ON o.user_id = u.id
                WHERE o.status = 'APROVADO'
                GROUP BY u.id, u.name
                ORDER BY totalSpent DESC, totalOrders DESC
                LIMIT 5
                """;
        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    @Override
    public List<Object[]> findAverageTicketByUser() {
        String sql = """
                SELECT
                    u.id AS userId,
                    u.name AS userName,
                    AVG(o.total_amount) AS averageTicket
                FROM users u
                INNER JOIN orders o ON o.user_id = u.id
                WHERE o.status = 'APROVADO'
                GROUP BY u.id, u.name
                ORDER BY averageTicket DESC
                """;
        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    @Override
    public Object[] findTotalRevenueByPeriod(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    COALESCE(SUM(o.total_amount), 0) AS totalRevenue,
                    COUNT(o.id) AS orderCount
                FROM orders o
                WHERE o.status = 'APROVADO'
                  AND DATE(o.order_date) BETWEEN :startDate AND :endDate
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return (Object[]) query.getSingleResult();
    }
}