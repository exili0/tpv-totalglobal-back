package com.ncortez.TPV_TotalGlobal.exception;

/**
 * Excepción de conflicto para devoluciones concurrentes o idempotencia inconsistente.
 */
public class RefundConflictException extends RuntimeException {
    public RefundConflictException(String message) {
        super(message);
    }
}
