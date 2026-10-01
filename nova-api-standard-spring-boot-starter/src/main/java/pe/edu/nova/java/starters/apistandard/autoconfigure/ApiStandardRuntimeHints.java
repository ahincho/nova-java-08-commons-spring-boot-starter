package pe.edu.nova.java.starters.apistandard.autoconfigure;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * Las pistas de reflexión del sobre de Nova para una imagen nativa (ADR-045).
 * <p>
 * Spring AOT registra los tipos que devuelve un controlador, pero el sobre lo arman
 * {@code ApiResponseInterceptor} y los puertos de errores, así que AOT nunca lo ve. Sin estas pistas,
 * Jackson no puede leer los componentes de los records del sobre en la imagen nativa y cada respuesta
 * termina en un 500. Se registra {@link ApiResponse} y, recursivamente, los tipos de sus componentes:
 * los errores, la metadata, los enlaces, el límite de tasa y la paginación.
 *
 * @author Nova Platform
 */
public class ApiStandardRuntimeHints implements RuntimeHintsRegistrar {

    private final BindingReflectionHintsRegistrar bindings = new BindingReflectionHintsRegistrar();

    /**
     * Crea el registrador.
     */
    public ApiStandardRuntimeHints() {
    }

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        bindings.registerReflectionHints(hints.reflection(), ApiResponse.class);
    }
}
