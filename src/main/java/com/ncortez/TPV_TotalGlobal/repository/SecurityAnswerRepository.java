package com.ncortez.TPV_TotalGlobal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ncortez.TPV_TotalGlobal.entity.SecurityAnswer;
import com.ncortez.TPV_TotalGlobal.entity.UserEntity;

/**
 * Repositorio de acceso a datos para la entidad SecurityAnswer.
 * Gestiona la persistencia y recuperación de respuestas de seguridad en la BBDD.
 */
@Repository
public interface SecurityAnswerRepository extends JpaRepository<SecurityAnswer, Long> {
    
    /**
     * Busca las respuestas de seguridad asociadas a un usuario.
     * @param user Usuario del que se buscan las respuestas
     * @return Optional con las respuestas de seguridad si existen
     */
	Optional<SecurityAnswer> findByUser(UserEntity user);
}