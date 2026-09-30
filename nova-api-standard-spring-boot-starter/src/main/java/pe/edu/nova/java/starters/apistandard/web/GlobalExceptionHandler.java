package pe.edu.nova.java.starters.apistandard.web;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import pe.edu.nova.java.libs.api.standard.error.ApiError;
import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * Manejador global de excepciones que convierte errores en respuestas
 * estándar {@link ApiResponse}.
 * <p>
 * Las excepciones propias de Spring MVC salen con su propio status: las que
 * implementan {@link ErrorResponse} (un header o un parámetro que falta, un
 * método o un tipo de contenido no soportado, una
 * {@code ResponseStatusException}) con el que declaran; un cuerpo ilegible o un
 * valor del tipo equivocado con 400; y un cuerpo o un parámetro que no pasa Bean
 * Validation con 400 y un error por campo.
 * </p>
 * <p>
 * Todo 5xx lleva el mensaje genérico y se registra en {@code error} con su
 * causa. Un 4xx se registra en {@code warn}, sin stack trace, porque es el
 * cliente el que mandó algo que no corresponde.
 * </p>
 *
 * @author Nova Platform
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Mensaje de todo 5xx, que no revela qué falló por dentro. */
    static final String INTERNAL_ERROR_MESSAGE = "Error interno del servidor";

    /** Mensaje de un error de validación que no trae uno propio. */
    private static final String INVALID_VALUE_MESSAGE = "Valor inválido";

    /** Paquete de las anotaciones con las que Spring MVC nombra un parámetro, como {@code @RequestParam}. */
    private static final String BIND_ANNOTATIONS_PACKAGE = "org.springframework.web.bind.annotation";

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
        return clientError(HttpStatus.NOT_FOUND, HttpHeaders.EMPTY, ex, response);
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
        return clientError(HttpStatus.BAD_REQUEST, HttpHeaders.EMPTY, ex, response);
    }

    /**
     * Maneja un cuerpo que no pasa Bean Validation (400), con un error por campo.
     *
     * @param ex la excepción
     * @return respuesta con error 400 y un {@link ApiError} de validación por cada campo inválido
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBody(MethodArgumentNotValidException ex) {
        List<ApiError> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(ApiError.validationError(error.getField(), messageOf(error)));
        }
        // Una restricción sobre el objeto entero no tiene campo
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(ApiError.validationError(null, messageOf(error)));
        }
        return clientError(HttpStatus.BAD_REQUEST, ex.getHeaders(), ex, ApiResponse.error(400, errors));
    }

    /**
     * Maneja un parámetro del método que no pasa Bean Validation (400), con un error por cada
     * restricción que falla. Si lo que falla es el valor de retorno, es un defecto del servicio y
     * sale como el 5xx que declara Spring.
     *
     * @param ex la excepción
     * @return respuesta con error 400 y un {@link ApiError} de validación por cada parámetro inválido
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidParameters(HandlerMethodValidationException ex) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.is5xxServerError()) {
            return serverError(status, ex.getHeaders(), ex);
        }
        List<ApiError> errors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String parameter = parameterName(result.getMethodParameter());
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                // Un objeto con @Valid trae sus errores por campo; una restricción sobre el
                // parámetro, como @Max, trae el error del parámetro
                String field = error instanceof FieldError fieldError ? fieldError.getField() : parameter;
                errors.add(ApiError.validationError(field, messageOf(error)));
            }
        }
        for (MessageSourceResolvable error : ex.getCrossParameterValidationResults()) {
            errors.add(ApiError.validationError(null, messageOf(error)));
        }
        return clientError(status, ex.getHeaders(), ex, ApiResponse.error(status.value(), errors));
    }

    /**
     * Maneja un cuerpo que no se puede leer, como un JSON mal formado (400).
     *
     * @param ex la excepción
     * @return respuesta con error 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // El mensaje de la excepción describe el parser y los tipos internos, así que no va al cliente
        ApiResponse<Void> response = ApiResponse.error(400, "No se pudo leer el cuerpo de la solicitud");
        return clientError(HttpStatus.BAD_REQUEST, HttpHeaders.EMPTY, ex, response);
    }

    /**
     * Maneja un parámetro con un valor del tipo equivocado, como un texto donde va un número (400).
     *
     * @param ex la excepción
     * @return respuesta con error 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ApiResponse<Void> response = ApiResponse.error(400, "Valor inválido para el parámetro '" + ex.getName() + "'");
        return clientError(HttpStatus.BAD_REQUEST, HttpHeaders.EMPTY, ex, response);
    }

    /**
     * Maneja cualquier excepción no capturada (500). Las excepciones de Spring MVC que
     * implementan {@link ErrorResponse} salen con el status que declaran.
     *
     * @param ex la excepción
     * @return respuesta con error 500, o con el status de la {@link ErrorResponse}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        // Un header o un parámetro que falta es 400, un método no soportado 405, un tipo de
        // contenido no soportado 415, y una ResponseStatusException trae el suyo
        if (ex instanceof ErrorResponse errorResponse) {
            return handleErrorResponse(ex, errorResponse);
        }
        logger.error("[Nova Platform] Error interno no manejado", ex);
        ApiResponse<Void> response = ApiResponse.error(500, INTERNAL_ERROR_MESSAGE);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * El mensaje de un error que no trae uno propio: el genérico si es un 5xx, y la frase estándar
     * del status si es un 4xx.
     *
     * @param status el status HTTP
     * @return el mensaje del error
     */
    static String defaultMessage(int status) {
        if (status >= 500) {
            return INTERNAL_ERROR_MESSAGE;
        }
        HttpStatus httpStatus = HttpStatus.resolve(status);
        return httpStatus != null ? httpStatus.getReasonPhrase() : "Error en la solicitud";
    }

    /**
     * Responde una excepción que declara su status con él, y con sus headers, como el
     * {@code Allow} de un 405.
     *
     * @param ex la excepción
     * @param errorResponse la misma excepción, vista como {@link ErrorResponse}
     * @return respuesta con el status de la excepción
     */
    private static ResponseEntity<ApiResponse<Void>> handleErrorResponse(Exception ex, ErrorResponse errorResponse) {
        HttpStatusCode status = errorResponse.getStatusCode();
        if (status.is5xxServerError()) {
            return serverError(status, errorResponse.getHeaders(), ex);
        }
        // El detail es el texto que Spring arma para el cliente, como "Required header 'X-Tenant'
        // is not present.", o el motivo de una ResponseStatusException
        String detail = errorResponse.getBody().getDetail();
        String message = detail != null && !detail.isBlank() ? detail : defaultMessage(status.value());
        return clientError(status, errorResponse.getHeaders(), ex, ApiResponse.error(status.value(), message));
    }

    /**
     * Responde un 4xx y lo registra en {@code warn}, sin stack trace. El registro lleva lo mismo que
     * ve el cliente, así que no repite los valores que mandó.
     *
     * @param status el status HTTP
     * @param headers los headers de la respuesta
     * @param ex la excepción
     * @param response el cuerpo de la respuesta
     * @return la respuesta
     */
    private static ResponseEntity<ApiResponse<Void>> clientError(
            HttpStatusCode status, HttpHeaders headers, Exception ex, ApiResponse<Void> response) {
        if (logger.isWarnEnabled()) {
            logger.warn("[Nova Platform] Solicitud rechazada con {} ({}): {}",
                    status.value(), ex.getClass().getSimpleName(), describe(response.errors()));
        }
        return ResponseEntity.status(status).headers(headers).body(response);
    }

    /**
     * Responde un 5xx con el mensaje genérico, y registra la causa completa en {@code error}.
     *
     * @param status el status HTTP
     * @param headers los headers de la respuesta
     * @param ex la excepción
     * @return la respuesta
     */
    private static ResponseEntity<ApiResponse<Void>> serverError(HttpStatusCode status, HttpHeaders headers, Exception ex) {
        logger.error("[Nova Platform] Error {} al atender la solicitud", status.value(), ex);
        ApiResponse<Void> response = ApiResponse.error(status.value(), INTERNAL_ERROR_MESSAGE);
        return ResponseEntity.status(status).headers(headers).body(response);
    }

    /**
     * El nombre con el que el cliente manda el parámetro: el que declara su {@code @RequestParam},
     * {@code @RequestHeader} o {@code @PathVariable}, o el del parámetro si no declara ninguno.
     *
     * @param parameter el parámetro del método
     * @return el nombre, o {@code null} si no se conoce
     */
    private static String parameterName(MethodParameter parameter) {
        for (Annotation annotation : parameter.getParameterAnnotations()) {
            if (annotation.annotationType().getPackageName().equals(BIND_ANNOTATIONS_PACKAGE)) {
                String name = MergedAnnotation.from(annotation).getValue("name", String.class).orElse("");
                if (!name.isEmpty()) {
                    return name;
                }
            }
        }
        return parameter.getParameterName();
    }

    /**
     * El mensaje de un error de validación, o uno genérico si la restricción no trae ninguno.
     *
     * @param error el error de validación
     * @return el mensaje
     */
    private static String messageOf(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message != null && !message.isBlank() ? message : INVALID_VALUE_MESSAGE;
    }

    /**
     * Resume los errores de una respuesta para el registro.
     *
     * @param errors los errores
     * @return cada error con su campo, si lo tiene
     */
    private static String describe(List<ApiError> errors) {
        return errors.stream()
                .map(error -> error.field() != null ? error.field() + ": " + error.message() : error.message())
                .collect(Collectors.joining("; "));
    }
}
