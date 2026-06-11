package com.ncortez.TPV_TotalGlobal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ncortez.TPV_TotalGlobal.entity.Product;

import jakarta.persistence.LockModeType;

/**
 * Repositorio de acceso a datos para la entidad Product.
 * Gestiona la persistencia y recuperación de productos en la BBDD.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Busca un producto por su código de barras único.
     * Utilizado principalmente en modo retail con escaneo.
     * @param barcode Código de barras a buscar
     * @return Optional con el producto si existe
     */
    Optional<Product> findByBarcode(String barcode);

    /**
     * Devuelve todos los productos activos.
     * Utilizados para visualización en el TPV.
     * @return Lista de productos activos
     */
    List<Product> findByActiveTrue();

    /**
     * Devuelve los productos activos de una categoría concreta.
     * Utilizados para llenar el grid de productos por categoría.
     * @param categoryId ID de la categoría
     * @return Lista de productos activos de esa categoría
     */
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    /**
     * Bloquea el producto en base de datos mientras se ajusta su stock.
     * Evita que dos operaciones simultaneas descuenten la misma cantidad.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

    /**
     * Comprueba si existe algún producto asociado a una categoría.
     * @param categoryId ID de la categoría
     * @return true si hay productos vinculados a la categoría
     */
    boolean existsByCategoryId(Long categoryId);
}
