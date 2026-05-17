package com.ncortez.TPV_TotalGlobal.controller;

import com.ncortez.TPV_TotalGlobal.dto.CategoryRequest;
import com.ncortez.TPV_TotalGlobal.entity.Category;
import com.ncortez.TPV_TotalGlobal.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de categorías de productos del TPV.
 * Proporciona endpoints para listar, crear, actualizar y eliminar categorías.
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:4200")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Obtiene todas las categorías del sistema (incluidas inactivas).
     * Operación para el panel de administración.
     * @return Lista de todas las categorías
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Obtiene solo las categorías activas.
     * Utilizadas para visualización en el TPV.
     * @return Lista de categorías activas
     */
    @GetMapping("/active")
    public ResponseEntity<List<Category>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    /**
     * Obtiene las categorías raíz activas (sin categoría padre).
     * Utilizadas para construir el menú principal del TPV.
     * @return Lista de categorías raíz activas
     */
    @GetMapping("/roots")
    public ResponseEntity<List<Category>> getRootCategories() {
        return ResponseEntity.ok(categoryService.getRootCategories());
    }

    /**
     * Obtiene una categoría por su identificador.
     * @param id Identificador de la categoría
     * @return Categoría si existe, 404 Not Found en caso contrario
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva categoría.
     * @param request DTO con los datos de la categoría (name, description, color, parentCategoryId)
     * @return Categoría creada o error 400 si falla
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest request) {
        try {
            Category category = categoryService.createCategory(
                request.getName(),
                request.getDescription(),
                request.getColor(),
                request.getParentCategoryId()
            );
            return ResponseEntity.ok(category);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Actualiza una categoría existente.
     * @param id Identificador de la categoría a actualizar
     * @param request DTO con los nuevos datos
     * @return Categoría actualizada o error 400 si falla
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        try {
            Category category = categoryService.updateCategory(
                id,
                request.getName(),
                request.getDescription(),
                request.getColor(),
                request.getParentCategoryId()
            );
            return ResponseEntity.ok(category);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Activa o desactiva una categoría sin eliminarla.
     * @param id Identificador de la categoría
     * @param request Request con el estado active (true/false)
     * @return Categoría actualizada o error 400 si falla
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<?> toggleCategoryActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        try {
            Category category = categoryService.toggleCategoryActive(id, request.isActive());
            return ResponseEntity.ok(category);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina una categoría del sistema.
     * @param id Identificador de la categoría a eliminar
     * @return Mensaje de éxito o error 400 si falla
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok("Categoría eliminada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * DTO que encapsula el estado de activación de una categoría.
     */
    public static class ActiveRequest {
        private boolean active;
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
