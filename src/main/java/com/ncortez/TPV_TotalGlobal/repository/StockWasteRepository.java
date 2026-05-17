package com.ncortez.TPV_TotalGlobal.repository;

import com.ncortez.TPV_TotalGlobal.entity.StockWaste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para registros de pérdidas de stock (desechos).
 * Permite obtener estadísticas sobre productos perdidos y razones.
 */
public interface StockWasteRepository extends JpaRepository<StockWaste, Long> {

    /**
     * Obtiene todas las pérdidas de un producto específico.
     * @param productId ID del producto
     * @return Lista de registros de pérdida ordenados por fecha descendente
     */
    List<StockWaste> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Obtiene pérdidas en un rango de fechas.
     * @param startDate Fecha inicio
     * @param endDate Fecha fin
     * @return Lista de pérdidas en ese rango
     */
    @Query("SELECT sw FROM StockWaste sw WHERE sw.createdAt BETWEEN :startDate AND :endDate ORDER BY sw.createdAt DESC")
    List<StockWaste> findWasteBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calcula la cantidad total de pérdidas por producto.
     * @param productId ID del producto
     * @return Total de unidades perdidas
     */
    @Query("SELECT COALESCE(SUM(sw.quantity), 0) FROM StockWaste sw WHERE sw.product.id = :productId")
    Integer getTotalWasteByProduct(@Param("productId") Long productId);

    /**
     * Calcula la cantidad total de pérdidas en todo el sistema.
     * @return Total de unidades perdidas
     */
    @Query("SELECT COALESCE(SUM(sw.quantity), 0) FROM StockWaste sw")
    Integer getTotalWaste();
}
