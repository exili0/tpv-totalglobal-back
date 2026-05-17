package com.ncortez.TPV_TotalGlobal.controller;

import com.ncortez.TPV_TotalGlobal.dto.ProductRequest;
import com.ncortez.TPV_TotalGlobal.entity.Product;
import com.ncortez.TPV_TotalGlobal.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de productos del TPV.
 * Proporciona endpoints para listar, buscar, crear, actualizar y eliminar productos.
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * Obtiene todos los productos del sistema (incluidos inactivos).
     * Operación para el panel de administración.
     * @return Lista de todos los productos
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * Obtiene solo los productos activos.
     * Utilizados para visualización en el TPV.
     * @return Lista de productos activos
     */
    @GetMapping("/active")
    public ResponseEntity<List<Product>> getActiveProducts() {
        return ResponseEntity.ok(productService.getActiveProducts());
    }

    /**
     * Obtiene los productos activos de una categoría.
     * Utilizado para llenar el grid de productos por categoría en el TPV.
     * @param categoryId ID de la categoría
     * @return Lista de productos activos de esa categoría
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    /**
     * Obtiene un producto por su identificador.
     * @param id Identificador del producto
     * @return Producto si existe, 404 Not Found en caso contrario
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca un producto por su código de barras.
     * Utilizado en modo retail con lector de códigos de barras.
     * @param barcode Código de barras a buscar
     * @return Producto si existe, 404 Not Found en caso contrario
     */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<?> getProductByBarcode(@PathVariable String barcode) {
        return productService.getProductByBarcode(barcode)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea un nuevo producto.
     * @param request DTO con los datos del producto
     * @return Producto creado o error 400 si falla
     */
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        try {
            Product product = productService.createProduct(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCostPrice(),
                request.getVatPercent(),
                request.getBarcode(),
                request.getImageUrl(),
                request.getStock(),
                request.getCategoryId()
            );
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Actualiza un producto existente.
     * @param id Identificador del producto a actualizar
     * @param request DTO con los nuevos datos
     * @return Producto actualizado o error 400 si falla
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        try {
            Product product = productService.updateProduct(
                id,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCostPrice(),
                request.getVatPercent(),
                request.getBarcode(),
                request.getImageUrl(),
                request.getStock(),
                request.getCategoryId()
            );
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Activa o desactiva un producto sin eliminarlo.
     * @param id Identificador del producto
     * @param request Request con el estado active (true/false)
     * @return Producto actualizado o error 400 si falla
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<?> toggleProductActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        try {
            Product product = productService.toggleProductActive(id, request.isActive());
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un producto del sistema.
     * @param id Identificador del producto a eliminar
     * @return Mensaje de éxito o error 400 si falla
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok("Producto eliminado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * DTO que encapsula el estado de activación de un producto.
     */
    public static class ActiveRequest {
        private boolean active;
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
