package com.ncortez.TPV_TotalGlobal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ncortez.TPV_TotalGlobal.entity.Product;
import com.ncortez.TPV_TotalGlobal.entity.Refund;
import com.ncortez.TPV_TotalGlobal.entity.StockMovement;
import com.ncortez.TPV_TotalGlobal.entity.StockWaste;
import com.ncortez.TPV_TotalGlobal.entity.enums.StockMovementType;
import com.ncortez.TPV_TotalGlobal.repository.ProductRepository;
import com.ncortez.TPV_TotalGlobal.repository.StockMovementRepository;
import com.ncortez.TPV_TotalGlobal.repository.StockWasteRepository;

/**
 * Servicio de gestión de stock automatizado.
 * Maneja desuentos, devoluciones, auditoría y estadísticas de pérdidas.
 */
@Service
public class StockService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private StockWasteRepository stockWasteRepository;

    private Product lockProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new RuntimeException("Producto inválido para control de stock");
        }

        return productRepository.findByIdForUpdate(product.getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + product.getId()));
    }

    /**
     * Descuenta stock cuando se agrega un producto al pedido.
     * Verifica que haya stock disponible antes de descontar.
     * Registra el movimiento en la auditoría.
     * 
     * @param product Producto a descontar
     * @param quantity Cantidad a descontar
     * @param saleOrderId ID de la orden de venta (para referencia)
     * @throws RuntimeException si no hay stock disponible
     */
    @Transactional
    public void deductStockForSale(Product product, Integer quantity, Long saleOrderId) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("La cantidad a descontar debe ser mayor que cero");
        }

        Product lockedProduct = lockProduct(product);

        if (lockedProduct.getStock() == null) {
            // Si el producto no tiene control de stock, no hacer nada
            return;
        }

        if (lockedProduct.getStock() < quantity) {
            throw new RuntimeException(
                String.format("Stock insuficiente para %s. Disponible: %d, Solicitado: %d",
                    lockedProduct.getName(), lockedProduct.getStock(), quantity)
            );
        }

        // Descontar stock
        lockedProduct.setStock(lockedProduct.getStock() - quantity);
        productRepository.saveAndFlush(lockedProduct);

        // Registrar movimiento de auditoría
        StockMovement movement = new StockMovement(lockedProduct, StockMovementType.SALE, quantity, saleOrderId);
        stockMovementRepository.save(movement);
    }

    /**
     * Incrementa el stock cuando se devuelve un producto al stock.
     * Registra el movimiento como RETURN en la auditoría.
     * 
     * @param product Producto a incrementar
     * @param quantity Cantidad a incrementar
     * @param refundId ID de la devolución (para referencia)
     */
    @Transactional
    public void returnStockFromRefund(Product product, Integer quantity, Long refundId) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("La cantidad a devolver debe ser mayor que cero");
        }

        Product lockedProduct = lockProduct(product);

        if (lockedProduct.getStock() == null) {
            // Si el producto no tiene control de stock, no hacer nada
            return;
        }

        // Incrementar stock
        lockedProduct.setStock(lockedProduct.getStock() + quantity);
        productRepository.saveAndFlush(lockedProduct);

        // Registrar movimiento de auditoría
        StockMovement movement = new StockMovement(lockedProduct, StockMovementType.RETURN, quantity, refundId);
        stockMovementRepository.save(movement);
    }

    /**
     * Devuelve stock reservado cuando una línea se reduce o elimina antes del cobro.
     * Mantiene trazabilidad usando el ID de la orden como referencia del movimiento.
     */
    @Transactional
    public void releaseReservedStockFromOrder(Product product, Integer quantity, Long saleOrderId) {
        if (product == null || quantity == null || quantity <= 0) {
            return;
        }

        Product lockedProduct = lockProduct(product);

        if (lockedProduct.getStock() == null) {
            // Si el producto no tiene control de stock, no hacer nada
            return;
        }

        lockedProduct.setStock(lockedProduct.getStock() + quantity);
        productRepository.saveAndFlush(lockedProduct);

        StockMovement movement = new StockMovement(lockedProduct, StockMovementType.RETURN, quantity, saleOrderId);
        stockMovementRepository.save(movement);
    }

    /**
     * Registra una pérdida de stock (desecho por devolución).
     * Se utiliza cuando el cliente devuelve un producto pero NO regresa al stock.
     * 
     * @param product Producto devuelto como desecho
     * @param refund Registro de devolución
     * @param quantity Cantidad considerada desecho
     * @param reason Razón de la pérdida (opcional)
     */
    @Transactional
    public void registerStockWaste(Product product, Refund refund, Integer quantity, String reason) {
        StockWaste waste = new StockWaste(product, quantity, reason);
        waste.setRefund(refund);
        stockWasteRepository.save(waste);

        // También registrar en auditoría con tipo ADJUSTMENT (salida)
        StockMovement movement = new StockMovement(product, StockMovementType.ADJUSTMENT, -quantity, refund.getId());
        stockMovementRepository.save(movement);
    }

    /**
     * Obtiene el histórico de movimientos de un producto.
     * 
     * @param productId ID del producto
     * @return Lista de movimientos ordenados por fecha descendente
     */
    public List<StockMovement> getProductStockHistory(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    /**
     * Obtiene las pérdidas de stock de un producto específico.
     * 
     * @param productId ID del producto
     * @return Lista de registros de pérdida
     */
    public List<StockWaste> getProductWasteHistory(Long productId) {
        return stockWasteRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    /**
     * Calcula el total de unidades perdidas por producto.
     * Útil para identificar qué productos tienen más desperdicio.
     * 
     * @return Mapa con productId -> totalWaste
     */
    public Map<Long, Integer> getTotalWasteByProduct() {
        List<Product> products = productRepository.findAll();
        return products.stream()
            .collect(Collectors.toMap(
                Product::getId,
                p -> stockWasteRepository.getTotalWasteByProduct(p.getId())
            ))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Obtiene estadísticas de pérdidas en un rango de fechas.
     * 
     * @param startDate Fecha inicio
     * @param endDate Fecha fin
     * @return Lista de pérdidas en ese rango
     */
    public List<StockWaste> getWasteStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return stockWasteRepository.findWasteBetweenDates(startDateTime, endDateTime);
    }

    /**
     * Obtiene el total de pérdidas en todo el sistema.
     * 
     * @return Cantidad total de unidades perdidas
     */
    public Integer getTotalWaste() {
        return stockWasteRepository.getTotalWaste();
    }

    /**
     * Obtiene movimientos de stock en un rango de fechas.
     * Útil para reportes diarios/mensuales.
     * 
     * @param startDate Fecha inicio
     * @param endDate Fecha fin
     * @return Lista de movimientos en ese rango
     */
    public List<StockMovement> getMovementsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return stockMovementRepository.findMovementsBetweenDates(startDateTime, endDateTime);
    }
}
