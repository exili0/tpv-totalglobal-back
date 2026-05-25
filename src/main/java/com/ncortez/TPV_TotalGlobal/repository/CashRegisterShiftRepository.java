package com.ncortez.TPV_TotalGlobal.repository;

import com.ncortez.TPV_TotalGlobal.entity.CashRegisterShift;
import com.ncortez.TPV_TotalGlobal.entity.enums.CashShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la persistencia de turnos de caja.
 */
public interface CashRegisterShiftRepository extends JpaRepository<CashRegisterShift, Long> {

    /**
     * Recupera el turno más reciente en estado OPEN.
     *
     * @param status estado del turno buscado
     * @return turno más reciente para ese estado, si existe
     */
    Optional<CashRegisterShift> findFirstByStatusOrderByOpenedAtDesc(CashShiftStatus status);

    /**
     * Devuelve todos los turnos ordenados por fecha de apertura descendente.
     *
     * @return histórico completo de turnos
     */
    List<CashRegisterShift> findAllByOrderByOpenedAtDesc();

    /**
     * Devuelve turnos abiertos entre dos fechas/horas de apertura (inclusive).
     *
     * @param start inicio del rango
     * @param end fin del rango
     * @return turnos cuya apertura está dentro del rango indicado
     */
    List<CashRegisterShift> findByOpenedAtBetweenOrderByOpenedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Devuelve turnos abiertos desde una fecha/horas concreta en adelante.
     *
     * @param start inicio del rango
     * @return turnos abiertos desde ese momento
     */
    List<CashRegisterShift> findByOpenedAtGreaterThanEqualOrderByOpenedAtDesc(LocalDateTime start);

    /**
     * Devuelve turnos abiertos hasta una fecha/horas concreta (inclusive).
     *
     * @param end fin del rango
     * @return turnos abiertos hasta ese momento
     */
    List<CashRegisterShift> findByOpenedAtLessThanEqualOrderByOpenedAtDesc(LocalDateTime end);
}
