package com.danrley.ecommerce.reports.repository;

import java.time.LocalDate;
import java.util.List;

// Nenhuma anotação @Repository aqui!
public interface ReportRepositoryCustom {

    List<Object[]> findTopBuyers();

    List<Object[]> findAverageTicketByUser();

    Object[] findTotalRevenueByPeriod(LocalDate startDate, LocalDate endDate);
}