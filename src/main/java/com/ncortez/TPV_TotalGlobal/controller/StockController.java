package com.ncortez.TPV_TotalGlobal.controller;

import com.ncortez.TPV_TotalGlobal.entity.StockMovement;
import com.ncortez.TPV_TotalGlobal.entity.StockWaste;
import com.ncortez.TPV_TotalGlobal.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de stock y estadísticas de pérdidas.
 * Proporciona endpoints para auditoría de stock, reportes de desechos y análisis de pérdidas.
 */
@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "http://localhost:4200")
public class StockController {

    @Autowired
    private StockService stockService;

    /**
     * Obtiene el histórico de movimientos de un producto específico.
     * Muestra todas las entradas/salidas de stock para trazabilidad completa.
     * 
     * @param productId ID del producto
     * @return Lista de movimientos ordenados por fecha descendente
     */
    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<?> getProductStockHistory(@PathVariable Long productId) {
        try {
            List<StockMovement> movements = stockService.getProductStockHistory(productId);
            return ResponseEntity.ok(movements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener histórico de stock: " + e.getMessage());
        }
    }

    /**
     * Obtiene movimientos de stock en un rango de fechas.
     * Útil para reportes diarios, semanales o mensuales.
     * 
     * @param startDate Fecha inicio (formato: yyyy-MM-dd)
     * @param endDate Fecha fin (formato: yyyy-MM-dd)
     * @return Lista de movimientos en el rango especificado
     */
    @GetMapping("/movements/range")
    public ResponseEntity<?> getMovementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<StockMovement> movements = stockService.getMovementsByDateRange(startDate, endDate);
            return ResponseEntity.ok(movements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener movimientos: " + e.getMessage());
        }
    }

    /**
     * Obtiene las pérdidas de stock de un producto específico.
     * Muestra todos los desechos registrados para ese producto.
     * 
     * @param productId ID del producto
     * @return Lista de registros de pérdida
     */
    @GetMapping("/waste/product/{productId}")
    public ResponseEntity<?> getProductWasteHistory(@PathVariable Long productId) {
        try {
            List<StockWaste> waste = stockService.getProductWasteHistory(productId);
            return ResponseEntity.ok(waste);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener historial de desechos: " + e.getMessage());
        }
    }

    /**
     * Obtiene estadísticas de pérdidas en un rango de fechas.
     * Permite analizar tendencias de desechos en el período especificado.
     * 
     * @param startDate Fecha inicio (formato: yyyy-MM-dd)
     * @param endDate Fecha fin (formato: yyyy-MM-dd)
     * @return Lista de pérdidas con detalles del producto y motivo
     */
    @GetMapping("/waste/range")
    public ResponseEntity<?> getWasteStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<StockWaste> waste = stockService.getWasteStatistics(startDate, endDate);
            return ResponseEntity.ok(waste);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener estadísticas de pérdidas: " + e.getMessage());
        }
    }

    /**
     * Obtiene un resumen de pérdidas por producto.
     * Identifica cuáles son los productos con más desechos.
     * 
     * @return Mapa con productId -> totalUnidadesPerdidas
     */
    @GetMapping("/waste/summary")
    public ResponseEntity<?> getWasteSummary() {
        try {
            Map<Long, Integer> wasteSummary = stockService.getTotalWasteByProduct();
            return ResponseEntity.ok(wasteSummary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener resumen de pérdidas: " + e.getMessage());
        }
    }

    /**
     * Obtiene el total de pérdidas en todo el sistema.
     * Métrica estratégica para diferenciación del TPV.
     * 
     * @return Objeto con la cantidad total de unidades perdidas
     */
    @GetMapping("/waste/total")
    public ResponseEntity<?> getTotalWaste() {
        try {
            Integer totalWaste = stockService.getTotalWaste();
            Map<String, Integer> response = new HashMap<>();
            response.put("totalWaste", totalWaste);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener total de pérdidas: " + e.getMessage());
        }
    }
}
