package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO para crear o actualizar categorías de productos del TPV.
 * Soporta jerarquía mediante parentCategoryId para organización multinivel.
 */
public class CategoryRequest {

    /** Nombre único de la categoría */
    private String name;
    /** Descripción detallada de la categoría */
    private String description;
    /** Color hexadecimal para visualización en el grid del TPV */
    private String color;
    /** ID de la categoría padre para crear jerarquía (null si es raíz) */
    private Long parentCategoryId;

    /**
     * Obtiene el nombre de la categoría.
     * @return Nombre de la categoría
     */
    public String getName() { return name; }
    /**
     * Establece el nombre de la categoría.
     * @param name Nombre de la categoría
     */
    public void setName(String name) { this.name = name; }

    /**
     * Obtiene la descripción de la categoría.
     * @return Descripción
     */
    public String getDescription() { return description; }
    /**
     * Establece la descripción de la categoría.
     * @param description Descripción
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Obtiene el color hexadecimal de la categoría.
     * @return Color en formato hexadecimal (ej. "#FF5733")
     */
    public String getColor() { return color; }
    /**
     * Establece el color hexadecimal de la categoría.
     * @param color Color en formato hexadecimal
     */
    public void setColor(String color) { this.color = color; }

    /**
     * Obtiene el ID de la categoría padre.
     * @return ID de la categoría padre o null si es raíz
     */
    public Long getParentCategoryId() { return parentCategoryId; }
    /**
     * Establece el ID de la categoría padre.
     * @param parentCategoryId ID de la categoría padre
     */
    public void setParentCategoryId(Long parentCategoryId) { this.parentCategoryId = parentCategoryId; }
}
