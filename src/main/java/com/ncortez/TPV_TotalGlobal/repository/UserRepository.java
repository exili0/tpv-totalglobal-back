package com.ncortez.TPV_TotalGlobal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ncortez.TPV_TotalGlobal.entity.UserEntity;

/**
 * Repositorio de acceso a datos para la entidad UserEntity.
 * Gestiona la persistencia y recuperación de usuarios en la BBDD.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    /**
     * Busca un usuario por su nombre de usuario único.
     * @param username Nombre de usuario a buscar
     * @return Optional con el usuario si existe, vacío en caso contrario
     */
    Optional<UserEntity> findByUsername(String username);
}