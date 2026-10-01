package pe.edu.nova.java.starters.apistandard.web;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import pe.edu.nova.java.libs.api.standard.error.ErrorPorts;
import pe.edu.nova.java.libs.api.standard.error.FieldError;
import pe.edu.nova.java.libs.api.standard.error.NovaError;
import pe.edu.nova.java.libs.api.standard.error.PlatformError;
import pe.edu.nova.java.libs.api.standard.error.SanitizedFailure;
import pe.edu.nova.java.libs.api.standard.error.SerializedError;

/**
 * El núcleo del manejo de errores en Spring MVC (ADR-031): responde cada error con los tres puertos de
 * {@link ErrorPorts}, y antes lo registra en el log una sola vez.
 * <p>
 * Un error de Nova ({@code DomainError}, {@code ApplicationError}, {@code InfrastructureError} o
 * {@code PlatformError}) sale con el status de su tipo. Una excepción propia de Spring MVC sale con su
 * status: un 4xx es {@code application}; un 502, un 503 o un 504, {@code infrastructure}; y cualquier
 * otro 5xx, {@code platform}. Cualquier otra excepción es un {@code PlatformError} y sale como 500.
 * <p>
 * El log lleva el {@code traceId}, la capa, el código y, si hay, el proveedor, como campos. Lo esperado
 * ({@code domain} y {@code application}) va en {@code warn}, sin stack trace; un incidente
 * ({@code infrastructure} y {@code platform}) va en {@code error}, con la causa. Los puertos nunca ven el
 * proveedor ni la causa: reciben un {@link SanitizedFailure}.
 * <p>
 * Si la petición no tiene {@code traceId}, el handler genera uno y lo escribe igual en el log y en el
 * cuerpo.
 *
 * @author Nova Platform
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Mensaje de un error de validación que no trae uno propio. */
    private static final String INVALID_VALUE_MESSAGE = "Valor inválido";

    /** Paquete de las anotaciones con las que Spring MVC nombra un parámetro, como {@code @RequestParam}. */
    private static final String BIND_ANNOTATIONS_PACKAGE = "org.springframework.web.bind.annotation";

    private final ErrorPorts ports;
    private final ErrorCounter counter;

    /**
     * Crea el manejador.
     *
     * @param ports   los tres puertos de errores: los de Nova o los del servicio
     * @param counter cuenta cada error respondido
     */
    public GlobalExceptionHandler(ErrorPorts ports, ErrorCounter counter) {
        this.ports = Objects.requireNonNull(ports, "ports es obligatorio");
        this.counter = Objects.requireNonNull(counter, "counter es obligatorio");
    }

    /**
     * Responde un error de Nova con el status de su tipo.
     *
     * @param error el error
     * @return la respuesta que deciden los puertos
     */
    @ExceptionHandler(NovaError.class)
    public ResponseEntity<Object> handleNovaError(NovaError error) {
        try (MDC.MDCCloseable ignored = ensureTraceId()) {
            SanitizedFailure failure = SanitizedFailure.of(error, ports.statusMapper().statusOf(error.type()));
            log(failure, error.upstream().orElse(null), error);
            return respond(failure, HttpHeaders.EMPTY);
        }
    }

    /**
     * Responde un recurso que no existe (404).
     *
     * @param ex la excepción
     * @return la respuesta 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNotFound(NoResourceFoundException ex) {
        return framework(404, "Recurso no encontrado: " + ex.getResourcePath(), List.of(), HttpHeaders.EMPTY, ex);
    }

    /**
     * Responde un cuerpo que no pasa Bean Validation (400), con un error por campo. Una restricción sobre
     * el objeto entero lleva el campo vacío.
     *
     * @param ex la excepción
     * @return la respuesta 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleInvalidBody(MethodArgumentNotValidException ex) {
        List<FieldError> fields = new ArrayList<>();
        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.add(FieldError.of(error.getField(), messageOf(error)));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            fields.add(FieldError.of("", messageOf(error)));
        }
        return framework(400, null, fields, ex.getHeaders(), ex);
    }

    /**
     * Responde un parámetro del método que no pasa Bean Validation (400), con un error por restricción.
     * Si lo que falla es el valor de retorno, es un defecto del servicio y sale con el 5xx que declara
     * Spring.
     *
     * @param ex la excepción
     * @return la respuesta
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> handleInvalidParameters(HandlerMethodValidationException ex) {
        HttpStatusCode status = ex.getStatusCode();
        List<FieldError> fields = new ArrayList<>();
        if (status.is4xxClientError()) {
            for (ParameterValidationResult result : ex.getParameterValidationResults()) {
                String parameter = parameterName(result.getMethodParameter());
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    // Un objeto con @Valid trae sus errores por campo; una restricción sobre el parámetro,
                    // como @Max, trae el error del parámetro
                    String field = error instanceof org.springframework.validation.FieldError fieldError
                            ? fieldError.getField()
                            : parameter;
                    fields.add(FieldError.of(field, messageOf(error)));
                }
            }
            for (MessageSourceResolvable error : ex.getCrossParameterValidationResults()) {
                fields.add(FieldError.of("", messageOf(error)));
            }
        }
        return framework(status.value(), null, fields, ex.getHeaders(), ex);
    }

    /**
     * Responde un cuerpo que no se puede leer, como un JSON mal formado (400).
     *
     * @param ex la excepción
     * @return la respuesta 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // El mensaje de la excepción describe el parser y los tipos internos, así que no va al cliente
        return framework(400, "No se pudo leer el cuerpo de la solicitud", List.of(), HttpHeaders.EMPTY, ex);
    }

    /**
     * Responde un parámetro con un valor del tipo equivocado, como un texto donde va un número (400).
     *
     * @param ex la excepción
     * @return la respuesta 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Valor inválido para el parámetro '" + ex.getName() + "'";
        return framework(400, message, List.of(), HttpHeaders.EMPTY, ex);
    }

    /**
     * Responde cualquier otra excepción. Las de Spring MVC que implementan {@link ErrorResponse} (un header
     * o un parámetro que falta, un método o un tipo de contenido no soportado, una
     * {@code ResponseStatusException}) salen con su status y sus headers; el resto es un
     * {@code PlatformError} y sale como 500.
     *
     * @param ex la excepción
     * @return la respuesta
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            // El detail es el texto que Spring arma para el cliente, como "Required header 'X-Tenant' is
            // not present.", o el motivo de una ResponseStatusException; en un 5xx no llega al cliente
            return framework(errorResponse.getStatusCode().value(), errorResponse.getBody().getDetail(),
                    List.of(), errorResponse.getHeaders(), ex);
        }
        return handleNovaError(PlatformError.internal(ex));
    }

    /**
     * Responde una excepción propia del framework, que ya trae su status.
     *
     * @param status  el status
     * @param message el mensaje para el cliente, que solo llega en un 4xx (puede ser null)
     * @param fields  los errores por campo, que solo llegan en un 4xx
     * @param headers los headers que declara la excepción, como el {@code Allow} de un 405
     * @param ex      la excepción, para el log
     * @return la respuesta que deciden los puertos
     */
    private ResponseEntity<Object> framework(int status, String message, List<FieldError> fields,
                                             HttpHeaders headers, Exception ex) {
        try (MDC.MDCCloseable ignored = ensureTraceId()) {
            SanitizedFailure failure = SanitizedFailure.ofStatus(status, null, message, fields, null);
            log(failure, null, ex);
            return respond(failure, headers);
        }
    }

    /**
     * Arma la respuesta con lo que deciden el catálogo y el serializador.
     *
     * @param failure el fallo saneado
     * @param extra   los headers propios de la excepción
     * @return la respuesta
     */
    private ResponseEntity<Object> respond(SanitizedFailure failure, HttpHeaders extra) {
        SerializedError serialized = ports.respond(failure);
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(extra);
        serialized.headers().forEach(headers::set);
        return ResponseEntity.status(serialized.status()).headers(headers).body(serialized.body());
    }

    /**
     * Escribe la línea de log del error, una sola vez y antes de llamar a los puertos, y lo cuenta.
     *
     * @param failure  el fallo saneado, que trae el status, la capa, el código y el {@code traceId}
     * @param upstream el proveedor que falló, si hay uno; solo va al log
     * @param cause    la excepción, cuyo stack trace va al log si es un incidente
     */
    private void log(SanitizedFailure failure, String upstream, Throwable cause) {
        boolean incident = failure.layer().isIncident();
        LoggingEventBuilder event = incident ? LOGGER.atError().setCause(cause) : LOGGER.atWarn();
        event = event.addKeyValue("traceId", failure.traceId().orElse(null))
                .addKeyValue("layer", failure.layer().label())
                .addKeyValue("code", failure.code())
                .addKeyValue("status", failure.status());
        if (upstream != null) {
            event = event.addKeyValue("upstream", upstream);
        }
        // Lo esperado registra lo mismo que ve el cliente; un incidente, la excepción con su mensaje. Los
        // campos van también en el texto, porque un log de consola sin formato estructurado no los muestra
        String detail = incident ? describe(cause) : failure.message();
        event.log("[Nova Platform] {} {} layer={}{} traceId={} ({}): {}",
                failure.status(), failure.code(), failure.layer().label(),
                upstream != null ? " upstream=" + upstream : "",
                failure.traceId().orElse("-"), cause.getClass().getSimpleName(), detail);
        counter.increment(failure.layer().label(), failure.code());
    }

    /**
     * Si la petición no tiene {@code traceId}, genera uno y lo deja en el MDC mientras se responde el
     * error, para que el log y el cuerpo lleven el mismo.
     *
     * @return lo que lo saca del MDC al terminar, o null si ya había uno
     */
    private static MDC.MDCCloseable ensureTraceId() {
        String current = MDC.get(MdcTraceIdSource.TRACE_ID_KEY);
        if (current != null && !current.isBlank()) {
            return null;
        }
        return MDC.putCloseable(MdcTraceIdSource.TRACE_ID_KEY, UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * Describe una excepción para el log, con su mensaje si lo tiene.
     *
     * @param cause la excepción
     * @return el mensaje, o el nombre de la clase si no trae uno
     */
    private static String describe(Throwable cause) {
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getName() : message;
    }

    /**
     * El nombre con el que el cliente manda el parámetro: el que declara su {@code @RequestParam},
     * {@code @RequestHeader} o {@code @PathVariable}, o el del parámetro si no declara ninguno.
     *
     * @param parameter el parámetro del método
     * @return el nombre, o vacío si no se conoce
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
        String name = parameter.getParameterName();
        return name != null ? name : "";
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
}
