package com.ncortez.TPV_TotalGlobal.repository;

import com.ncortez.TPV_TotalGlobal.entity.BusinessTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la persistencia de mesas del negocio.
 */
public interface BusinessTableRepository extends JpaRepository<BusinessTable, Long> {
    Optional<BusinessTable> findByTableNumber(Integer tableNumber);
    List<BusinessTable> findByActiveTrueOrderByTableNumberAsc();
}
