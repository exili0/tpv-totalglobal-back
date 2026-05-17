package com.ncortez.TPV_TotalGlobal.repository;

import com.ncortez.TPV_TotalGlobal.entity.BusinessTable;
import com.ncortez.TPV_TotalGlobal.entity.SaleOrder;
import com.ncortez.TPV_TotalGlobal.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la persistencia de órdenes de venta.
 */
public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {
    Optional<SaleOrder> findFirstByTableAndStatus(BusinessTable table, OrderStatus status);
    List<SaleOrder> findByStatus(OrderStatus status);
    List<SaleOrder> findByStatusAndClosedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);
}
