package pe.edu.nova.java.starters.apistandard.web;

import java.util.Optional;

import org.slf4j.MDC;

import pe.edu.nova.java.libs.api.standard.error.TraceIdSource;

/**
 * El {@code traceId} de la petición en curso, tomado del MDC de SLF4J.
 * <p>
 * Es donde lo deja la observabilidad de Spring Boot y la de Nova, con la clave {@value #TRACE_ID_KEY}. Un
 * error de Nova lo captura al nacer a través de esta fuente, que se registra en
 * {@code META-INF/services}.
 *
 * @author Nova Platform
 */
public final class MdcTraceIdSource implements TraceIdSource {

    /** La clave del MDC con el identificador de traza. */
    public static final String TRACE_ID_KEY = "traceId";

    /** Crea la fuente; la instancia el {@code ServiceLoader}. */
    public MdcTraceIdSource() {
    }

    @Override
    public Optional<String> currentTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return traceId == null || traceId.isBlank() ? Optional.empty() : Optional.of(traceId);
    }
}
