package com.ncortez.TPV_TotalGlobal;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ncortez.TPV_TotalGlobal.entity.SecurityAnswer;
import com.ncortez.TPV_TotalGlobal.entity.UserEntity;
import com.ncortez.TPV_TotalGlobal.entity.enums.Role;
import com.ncortez.TPV_TotalGlobal.entity.BusinessTable;
import com.ncortez.TPV_TotalGlobal.repository.SecurityAnswerRepository;
import com.ncortez.TPV_TotalGlobal.repository.UserRepository;
import com.ncortez.TPV_TotalGlobal.entity.Category;
import com.ncortez.TPV_TotalGlobal.entity.Product;
import com.ncortez.TPV_TotalGlobal.repository.BusinessTableRepository;
import com.ncortez.TPV_TotalGlobal.repository.CategoryRepository;
import com.ncortez.TPV_TotalGlobal.repository.ProductRepository;

import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;

/**
 * Componente de inicialización de datos para pruebas del TPV TotalGlobal.
 * Crea automáticamente usuarios, categorías y productos de demostración al iniciar la aplicación.
 */
@Component
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAnswerRepository securityRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BusinessTableRepository businessTableRepository;

    /**
     * Punto de entrada de inicialización de datos ejecutado después de la construcción del componente.
     * Crea usuarios de prueba con respuestas de seguridad y categorías/productos para demostración.
     */
    @PostConstruct
    @Transactional
    public void init() {
        System.out.println("--- DATA INITIALIZER: Iniciando carga de datos TFG ---");

        // 1. Crear usuarios de prueba con sus respuestas de seguridad
        ensureUserWithAnswers("nico", "1234", "Nicolás", "Cortez", "nico@tpv.com", Role.COMMON_USER, "Karina", "Cristina");
        ensureUserWithAnswers("admin", "admin", "Admin", "Sistema", "admin@tpv.com", Role.ADMIN, "admin", "admin");

        // 2. Crear categorías y productos de demostración
        initializeCategoriesAndProducts();

        // 3. Create operational tables (including table 0 as bar)
        initializeBusinessTables();

        System.out.println("--- DATA INITIALIZER: Datos cargados correctamente ---");
    }

    /**
     * Garantiza que un usuario exista con sus respuestas de seguridad asociadas.
     * Si el usuario no existe, lo crea con los datos proporcionados y lo marca como no bloqueado.
     * Si el usuario existe pero no tiene respuestas de seguridad, las crea.
     * @param username Nombre de usuario único
     * @param password Contraseña en texto plano (debería cifrarse en producción)
     * @param name Nombre del usuario
     * @param lastname Apellido del usuario
     * @param email Correo electrónico del usuario
     * @param role Rol del usuario (ADMIN, COMMON_USER)
     * @param firstAnswer Respuesta a la primera pregunta de seguridad
     * @param secondAnswer Respuesta a la segunda pregunta de seguridad
     */
    private void ensureUserWithAnswers(String username, String password, String name, String lastname, String email,
            Role role, String firstAnswer, String secondAnswer) {
        
        UserEntity user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity(username, password, name, lastname, email, role, true);
                    return userRepository.save(newUser);
                });

        boolean hasAnswers = securityRepository.findByUser(user).isPresent();
        if (!hasAnswers) {
            SecurityAnswer answers = new SecurityAnswer(firstAnswer, secondAnswer, user);
            // Establecemos la relación en ambos sentidos
            user.setSecurityAnswer(answers);
            securityRepository.save(answers);
            userRepository.save(user);
            System.out.println(">> Creado usuario y seguridad para: " + username);
        }
    }

    /**
     * Inicializa categorías y productos de demostración para el caso de uso de hostelería.
     * Crea 4 categorías principales (Bebidas, Entrantes, Carnes, Postres)
     * y 2-3 productos en cada una con precios y descripciones realistas.
     */
    private void initializeCategoriesAndProducts() {
        // Si ya existen categorías, no las recreamos
        if (categoryRepository.count() > 0) {
            System.out.println(">> Categorías y productos ya existen, omitiendo inicialización");
            return;
        }

        // Crear categorías raíz
        Category bebidas = new Category("Bebidas", "Bebidas variadas para acompañar las comidas", "#FF5733");
        Category entrantes = new Category("Entrantes", "Platos de entrada ligeros", "#33FF57");
        Category carnes = new Category("Carnes", "Platos principales con proteínas", "#3357FF");
        Category postres = new Category("Postres", "Dulces y postres para finalizar", "#FFFF33");

        bebidas.setActive(true);
        entrantes.setActive(true);
        carnes.setActive(true);
        postres.setActive(true);

        categoryRepository.save(bebidas);
        categoryRepository.save(entrantes);
        categoryRepository.save(carnes);
        categoryRepository.save(postres);

        System.out.println(">> Categorías creadas: Bebidas, Entrantes, Carnes, Postres");

        // Crear productos para Bebidas
        Product agua = new Product("Agua mineral", "Botella de agua mineral 500ml", new BigDecimal("2.50"), 21, bebidas);
        agua.setActive(true);
        agua.setCostPrice(new BigDecimal("0.70"));
        productRepository.save(agua);

        Product vino = new Product("Vino tinto", "Copa de vino tinto de la casa", new BigDecimal("4.50"), 21, bebidas);
        vino.setActive(true);
        vino.setCostPrice(new BigDecimal("1.40"));
        productRepository.save(vino);

        Product cerveza = new Product("Cerveza", "Jarra de cerveza 500ml", new BigDecimal("3.75"), 21, bebidas);
        cerveza.setActive(true);
        cerveza.setCostPrice(new BigDecimal("1.20"));
        productRepository.save(cerveza);

        // Crear productos para Entrantes
        Product tabla = new Product("Tabla de quesos", "Tabla variada con quesos locales", new BigDecimal("12.99"), 21, entrantes);
        tabla.setActive(true);
        tabla.setCostPrice(new BigDecimal("5.20"));
        productRepository.save(tabla);

        Product camarones = new Product("Camarones al ajillo", "Camarones salteados con ajo y perejil", new BigDecimal("10.50"), 21, entrantes);
        camarones.setActive(true);
        camarones.setCostPrice(new BigDecimal("4.80"));
        productRepository.save(camarones);

        // Crear productos para Carnes
        Product carne = new Product("Carne a la parrilla", "Filete de carne a la parrilla con verduras", new BigDecimal("19.99"), 21, carnes);
        carne.setActive(true);
        carne.setCostPrice(new BigDecimal("8.90"));
        productRepository.save(carne);

        Product pollo = new Product("Pechuga de pollo", "Pechuga de pollo rellena de jamón y queso", new BigDecimal("14.75"), 21, carnes);
        pollo.setActive(true);
        pollo.setCostPrice(new BigDecimal("6.10"));
        productRepository.save(pollo);

        Product pescado = new Product("Lubina a la sal", "Lubina entera cocida a la sal", new BigDecimal("18.50"), 21, carnes);
        pescado.setActive(true);
        pescado.setCostPrice(new BigDecimal("8.20"));
        productRepository.save(pescado);

        // Crear productos para Postres
        Product flan = new Product("Flan de huevo", "Flan casero con caramelo", new BigDecimal("4.50"), 21, postres);
        flan.setActive(true);
        flan.setCostPrice(new BigDecimal("1.30"));
        productRepository.save(flan);

        Product tiramisú = new Product("Tiramisú", "Postre italiano clásico", new BigDecimal("5.75"), 21, postres);
        tiramisú.setActive(true);
        tiramisú.setCostPrice(new BigDecimal("2.10"));
        productRepository.save(tiramisú);

        System.out.println(">> Productos de demostración creados: 11 productos en total");
    }

    /**
     * Initializes business tables for hospitality operation.
     * Table 0 is reserved as Bar to unify bar and table workflows.
     */
    private void initializeBusinessTables() {
        if (businessTableRepository.count() > 0) {
            System.out.println(">> Tables already exist, skipping initialization");
            return;
        }

        BusinessTable bar = new BusinessTable(0, "Bar", 10);
        businessTableRepository.save(bar);

        for (int i = 1; i <= 12; i++) {
            businessTableRepository.save(new BusinessTable(i, "Table " + i, 4));
        }

        System.out.println(">> Tables created: Bar (Table 0) + 12 tables");
    }
}