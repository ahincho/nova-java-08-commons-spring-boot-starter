package pe.edu.nova.java.starters.apistandard.web;

import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Un manejador de excepciones del servicio de las pruebas, que contesta con un cuerpo propio. Lleva
 * la precedencia más alta para que su excepción no la tome el manejador de {@code Exception} del
 * starter, sin depender del orden en que se registran los beans.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ItemExceptionHandler {

    /**
     * Contesta 423 con quién tiene bloqueado el ítem.
     *
     * @param ex la excepción
     * @return el cuerpo propio del servicio
     */
    @ExceptionHandler(ItemLockedException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public Map<String, String> handleLocked(ItemLockedException ex) {
        return Map.of("lockedBy", ex.getMessage());
    }

    /** Un ítem que otro proceso tiene bloqueado. */
    static class ItemLockedException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /**
         * Crea la excepción.
         *
         * @param lockedBy quién tiene bloqueado el ítem
         */
        ItemLockedException(String lockedBy) {
            super(lockedBy);
        }
    }
}
