package com.ncortez.TPV_TotalGlobal.repository;

import com.ncortez.TPV_TotalGlobal.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Category.
 * Gestiona la persistencia y recuperación de categorías de productos en la BBDD.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Busca una categoría por su nombre único.
     * @param name Nombre de la categoría a buscar
     * @return Optional con la categoría si existe
     */
    Optional<Category> findByName(String name);

    /**
     * Devuelve solo las categorías activas.
     * @return Lista de categorías activas
     */
    List<Category> findByActiveTrue();

    /**
     * Devuelve las categorías raíz activas (sin categoría padre).
     * Utilizadas para construir el menú principal del TPV.
     * @return Lista de categorías raíz activas
     */
    List<Category> findByParentCategoryIsNullAndActiveTrue();

    /**
     * Comprueba si existe alguna subcategoría asociada a una categoría padre.
     * @param parentCategoryId ID de la categoría padre
     * @return true si hay subcategorías vinculadas
     */
    boolean existsByParentCategoryId(Long parentCategoryId);
}
