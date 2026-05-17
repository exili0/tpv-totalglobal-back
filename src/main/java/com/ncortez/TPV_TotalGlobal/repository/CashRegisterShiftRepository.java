package com.ncortez.TPV_TotalGlobal.repository;

import com.ncortez.TPV_TotalGlobal.entity.CashRegisterShift;
import com.ncortez.TPV_TotalGlobal.entity.enums.CashShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para la persistencia de turnos de caja.
 */
public interface CashRegisterShiftRepository extends JpaRepository<CashRegisterShift, Long> {
    Optional<CashRegisterShift> findFirstByStatusOrderByOpenedAtDesc(CashShiftStatus status);
}
