package pe.edu.nova.java.starters.apistandard.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * Manejador global de excepciones que convierte errores en respuestas
 * estándar {@link ApiResponse}.
 *
 * @author Nova Platform
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Crea una nueva instancia del manejador de excepciones. */
    public GlobalExceptionHandler() {
    }

    /**
     * Maneja excepciones de recurso no encontrado (404).
     *
     * @param ex la excepción
     * @return respuesta con error 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        ApiResponse<Void> response = ApiResponse.error(404, "Recurso no encontrado: " + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Maneja IllegalArgumentException (400).
     *
     * @param ex la excepción
     * @return respuesta con error 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.error(400, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maneja cualquier excepción no capturada (500).
     *
     * @param ex la excepción
     * @return respuesta con error 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("[Nova Platform] Error interno no manejado", ex);
        ApiResponse<Void> response = ApiResponse.error(500, "Error interno del servidor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
