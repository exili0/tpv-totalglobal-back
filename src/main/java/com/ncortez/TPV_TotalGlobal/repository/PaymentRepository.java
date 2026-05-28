package com.ncortez.TPV_TotalGlobal.repository;

import com.ncortez.TPV_TotalGlobal.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la persistencia de cobros.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPaidAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
            select p
            from Payment p
            left join fetch p.saleOrder so
            left join fetch so.table t
            order by p.paidAt desc
            """)
    List<Payment> findAllWithSaleOrderAndTableOrderByPaidAtDesc();

    @Query("""
            select p
            from Payment p
            left join fetch p.saleOrder so
            left join fetch so.table t
            left join fetch so.orderLines lines
            where p.id = :paymentId
            """)
    Optional<Payment> findByIdWithTicketDetail(@Param("paymentId") Long paymentId);

    @Query("""
            select distinct p
            from Payment p
            left join fetch p.saleOrder so
            left join fetch so.orderLines lines
            left join fetch lines.product
            where p.paidAt between :start and :end
            """)
    List<Payment> findByPaidAtBetweenWithOrderLinesAndProducts(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
