package com.ncortez.TPV_TotalGlobal.service;

import com.ncortez.TPV_TotalGlobal.entity.Category;
import com.ncortez.TPV_TotalGlobal.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de lógica de negocio para la gestión de categorías de productos.
 * Encapsula operaciones CRUD y consultas especializadas para categorías.
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Obtiene todas las categorías del sistema (incluidas las inactivas).
     * Utilizado para el panel de administración.
     * @return Lista de todas las categorías
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Obtiene solo las categorías activas.
     * Utilizadas para visualización en el TPV.
     * @return Lista de categorías activas
     */
    public List<Category> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    /**
     * Obtiene las categorías raíz activas (sin categoría padre).
     * Utilizadas para construir el menú principal del TPV.
     * @return Lista de categorías raíz activas
     */
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentCategoryIsNullAndActiveTrue();
    }

    /**
     * Obtiene una categoría por su identificador.
     * @param id Identificador de la categoría
     * @return Optional con la categoría si existe
     */
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    /**
     * Crea una nueva categoría de productos.
     * @param name Nombre único de la categoría
     * @param description Descripción de la categoría
     * @param color Color hexadecimal para visualización (#RRGGBB)
     * @param parentCategoryId ID de la categoría padre (null para categorías raíz)
     * @return Categoría creada
     * @throws RuntimeException Si el nombre ya existe o la categoría padre no existe
     */
    public Category createCategory(String name, String description, String color, Long parentCategoryId) {
        if (categoryRepository.findByName(name).isPresent()) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }

        Category category = new Category(name, description, color);

        if (parentCategoryId != null) {
            Category parent = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new RuntimeException("Categoría padre no encontrada"));
            category.setParentCategory(parent);
        }

        return categoryRepository.save(category);
    }

    /**
     * Actualiza una categoría existente.
     * @param id Identificador de la categoría a actualizar
     * @param name Nuevo nombre (null para no cambiar)
     * @param description Nueva descripción (null para no cambiar)
     * @param color Nuevo color (null para no cambiar)
     * @param parentCategoryId Nuevo padre (null para hacerla raíz)
     * @return Categoría actualizada
     * @throws RuntimeException Si la categoría o el padre no existen
     */
    public Category updateCategory(Long id, String name, String description, String color, Long parentCategoryId) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Verificar nombre único si ha cambiado
        if (name != null && !name.equals(category.getName())) {
            if (categoryRepository.findByName(name).isPresent()) {
                throw new RuntimeException("Ya existe una categoría con ese nombre");
            }
            category.setName(name);
        }

        if (description != null) category.setDescription(description);
        if (color != null) category.setColor(color);

        if (parentCategoryId != null) {
            // Evitar que una categoría sea su propio padre
            if (parentCategoryId.equals(id)) {
                throw new RuntimeException("Una categoría no puede ser su propia categoría padre");
            }
            Category parent = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new RuntimeException("Categoría padre no encontrada"));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        return categoryRepository.save(category);
    }

    /**
     * Activa o desactiva una categoría sin eliminarla.
     * @param id Identificador de la categoría
     * @param active true para activar, false para desactivar
     * @return Categoría actualizada
     * @throws RuntimeException Si la categoría no existe
     */
    public Category toggleCategoryActive(Long id, boolean active) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        category.setActive(active);
        return categoryRepository.save(category);
    }

    /**
     * Elimina una categoría del sistema.
     * @param id Identificador de la categoría a eliminar
     * @throws RuntimeException Si la categoría no existe
     */
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada");
        }
        categoryRepository.deleteById(id);
    }
}
