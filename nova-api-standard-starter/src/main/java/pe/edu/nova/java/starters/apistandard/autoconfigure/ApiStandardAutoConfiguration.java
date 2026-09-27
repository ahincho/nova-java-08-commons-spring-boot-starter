package pe.edu.nova.java.starters.apistandard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import pe.edu.nova.java.starters.apistandard.web.ApiResponseInterceptor;
import pe.edu.nova.java.starters.apistandard.web.GlobalExceptionHandler;

/**
 * Auto-configuración del estándar de respuestas API.
 * <p>
 * Registra automáticamente un interceptor que envuelve las respuestas
 * en {@code ApiResponse} y un manejador global de excepciones.
 * </p>
 *
 * @author Nova Platform
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "nova.api-standard",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ApiStandardAutoConfiguration {

    /** Crea una nueva instancia de la auto-configuración. */
    public ApiStandardAutoConfiguration() {
    }

    /**
     * Registra el interceptor que envuelve respuestas en ApiResponse.
     *
     * @return interceptor de respuestas
     */
    @Bean
    public ApiResponseInterceptor apiResponseInterceptor() {
        return new ApiResponseInterceptor();
    }

    /**
     * Registra el manejador global de excepciones.
     *
     * @return manejador de excepciones
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
