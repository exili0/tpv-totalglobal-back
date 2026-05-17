package com.ncortez.TPV_TotalGlobal.repository;

import com.ncortez.TPV_TotalGlobal.entity.StockMovement;
import com.ncortez.TPV_TotalGlobal.entity.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para registros de movimientos de stock.
 * Facilita auditoría y trazabilidad completa de cambios de stock.
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Obtiene todos los movimientos de un producto específico.
     * @param productId ID del producto
     * @return Lista de movimientos ordenados por fecha descendente
     */
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Obtiene movimientos de un tipo específico.
     * @param movementType Tipo de movimiento (SALE, RETURN, ADJUSTMENT)
     * @return Lista de movimientos del tipo especificado
     */
    List<StockMovement> findByMovementType(StockMovementType movementType);

    /**
     * Obtiene movimientos en un rango de fechas.
     * @param startDate Fecha inicio
     * @param endDate Fecha fin
     * @return Lista de movimientos en ese rango
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.createdAt BETWEEN :startDate AND :endDate ORDER BY sm.createdAt DESC")
    List<StockMovement> findMovementsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Obtiene movimientos de ventas (SALE) por referencia (saleOrderId).
     * @param referenceId ID de la orden de venta
     * @return Lista de movimientos de venta para esa orden
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementType = 'SALE' AND sm.referenceId = :referenceId")
    List<StockMovement> findSaleMovementsByOrderId(@Param("referenceId") Long referenceId);
}
