package com.ncortez.TPV_TotalGlobal.service;

import com.ncortez.TPV_TotalGlobal.entity.Category;
import com.ncortez.TPV_TotalGlobal.entity.Product;
import com.ncortez.TPV_TotalGlobal.repository.CategoryRepository;
import com.ncortez.TPV_TotalGlobal.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de lógica de negocio para la gestión de productos del TPV.
 * Encapsula operaciones CRUD, validaciones y consultas especializadas para productos.
 */
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Obtiene todos los productos del sistema (incluidos los inactivos).
     * Utilizado para el panel de administración.
     * @return Lista de todos los productos
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Obtiene solo los productos activos.
     * Utilizados para visualización en el TPV.
     * @return Lista de productos activos
     */
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    /**
     * Obtiene los productos activos de una categoría concreta.
     * Utilizados para llenar el grid de productos por categoría en el TPV.
     * @param categoryId ID de la categoría
     * @return Lista de productos activos de esa categoría
     */
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId);
    }

    /**
     * Obtiene un producto por su identificador.
     * @param id Identificador del producto
     * @return Optional con el producto si existe
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Busca un producto por su código de barras.
     * Utilizado en modo retail con escaneo.
     * @param barcode Código de barras a buscar
     * @return Optional con el producto si existe
     */
    public Optional<Product> getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    /**
     * Crea un nuevo producto en el sistema.
     * @param name Nombre del producto
     * @param description Descripción
     * @param price Precio base sin IVA
     * @param vatPercent Porcentaje de IVA aplicable
     * @param barcode Código de barras (opcional)
     * @param imageUrl URL de la imagen (opcional)
     * @param stock Stock disponible (opcional, sólo retail)
     * @param categoryId ID de la categoría
     * @return Producto creado
     * @throws RuntimeException Si la categoría no existe o el código de barras es duplicado
     */
    public Product createProduct(String name, String description, BigDecimal price, BigDecimal costPrice,
                                  int vatPercent, String barcode, String imageUrl,
                                  Integer stock, Long categoryId) {
        // Verificar código de barras único si se proporciona
        if (barcode != null && !barcode.isBlank()) {
            if (productRepository.findByBarcode(barcode).isPresent()) {
                throw new RuntimeException("Ya existe un producto con ese código de barras");
            }
        }

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Product product = new Product(name, description, price, vatPercent, category);
        product.setCostPrice(costPrice);
        product.setBarcode(barcode != null && !barcode.isBlank() ? barcode : null);
        product.setImageUrl(imageUrl);
        product.setStock(stock);

        return productRepository.save(product);
    }

    /**
     * Actualiza un producto existente.
     * @param id Identificador del producto a actualizar
     * @param name Nuevo nombre (opcional)
     * @param description Nueva descripción (opcional)
     * @param price Nuevo precio (opcional)
     * @param vatPercent Nuevo porcentaje de IVA
     * @param barcode Nuevo código de barras (opcional)
     * @param imageUrl Nueva URL de imagen (opcional)
     * @param stock Nuevo stock (opcional)
     * @param categoryId Nueva categoría (opcional)
     * @return Producto actualizado
     * @throws RuntimeException Si el producto o categoría no existen
     */
    public Product updateProduct(Long id, String name, String description, BigDecimal price, BigDecimal costPrice,
                                  int vatPercent, String barcode, String imageUrl,
                                  Integer stock, Long categoryId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Verificar código de barras único si ha cambiado
        if (barcode != null && !barcode.isBlank() && !barcode.equals(product.getBarcode())) {
            if (productRepository.findByBarcode(barcode).isPresent()) {
                throw new RuntimeException("Ya existe un producto con ese código de barras");
            }
        }

        if (name != null) product.setName(name);
        if (description != null) product.setDescription(description);
        if (price != null) product.setPrice(price);
        if (costPrice != null) product.setCostPrice(costPrice);
        product.setVatPercent(vatPercent);
        product.setBarcode(barcode != null && !barcode.isBlank() ? barcode : null);
        product.setImageUrl(imageUrl);
        product.setStock(stock);

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            product.setCategory(category);
        }

        return productRepository.save(product);
    }

    /**
     * Activa o desactiva un producto sin eliminarlo.
     * @param id Identificador del producto
     * @param active true para activar, false para desactivar
     * @return Producto actualizado
     * @throws RuntimeException Si el producto no existe
     */
    public Product toggleProductActive(Long id, boolean active) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        product.setActive(active);
        return productRepository.save(product);
    }

    /**
     * Elimina un producto del sistema.
     * @param id Identificador del producto a eliminar
     * @throws RuntimeException Si el producto no existe
     */
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado");
        }
        productRepository.deleteById(id);
    }
}
