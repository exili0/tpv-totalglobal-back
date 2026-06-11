package com.ncortez.TPV_TotalGlobal.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ncortez.TPV_TotalGlobal.entity.Refund;

/**
 * Repositorio para la persistencia de devoluciones.
 */
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findAllByOrderByRefundedAtDesc();

    Optional<Refund> findFirstByPaymentIdAndIdempotencyKey(Long paymentId, String idempotencyKey);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment.id = :paymentId")
    BigDecimal sumAmountByPaymentId(@Param("paymentId") Long paymentId);

    @Query("select r.payment.id, coalesce(sum(r.amount), 0) from Refund r group by r.payment.id")
    List<Object[]> sumAmountGroupedByPayment();

        @Query("""
                        select coalesce(sum(r.refundedQuantity), 0)
                        from Refund r
                        where r.payment.id = :paymentId
                            and r.saleOrderLine.id = :saleOrderLineId
                        """)
        Long sumRefundedQuantityByPaymentAndLine(@Param("paymentId") Long paymentId, @Param("saleOrderLineId") Long saleOrderLineId);

        @Query("""
                        select r.saleOrderLine.id, coalesce(sum(r.refundedQuantity), 0)
                        from Refund r
                        where r.payment.id = :paymentId
                            and r.saleOrderLine is not null
                        group by r.saleOrderLine.id
                        """)
        List<Object[]> sumRefundedQuantityGroupedByLine(@Param("paymentId") Long paymentId);

        @Query("""
                        select distinct r
                        from Refund r
                        left join fetch r.payment p
                        left join fetch r.saleOrderLine l
                        left join fetch l.product
                        where r.refundedAt between :start and :end
                        """)
        List<Refund> findByRefundedAtBetweenWithDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
