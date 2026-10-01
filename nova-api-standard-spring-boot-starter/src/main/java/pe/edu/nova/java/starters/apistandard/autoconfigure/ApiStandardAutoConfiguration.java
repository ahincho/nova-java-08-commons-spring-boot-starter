package pe.edu.nova.java.starters.apistandard.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.edu.nova.java.libs.api.standard.error.ErrorCatalog;
import pe.edu.nova.java.libs.api.standard.error.ErrorPorts;
import pe.edu.nova.java.libs.api.standard.error.ErrorSerializer;
import pe.edu.nova.java.libs.api.standard.error.ErrorStatusMapper;
import pe.edu.nova.java.libs.api.standard.error.NovaErrorCatalog;
import pe.edu.nova.java.libs.api.standard.error.NovaErrorSerializer;
import pe.edu.nova.java.libs.api.standard.error.NovaErrorStatusMapper;
import pe.edu.nova.java.starters.apistandard.web.ApiResponseInterceptor;
import pe.edu.nova.java.starters.apistandard.web.ErrorCounter;
import pe.edu.nova.java.starters.apistandard.web.GlobalExceptionHandler;

/**
 * Auto-configuración del estándar de API de Nova Platform: el sobre de las respuestas y el manejo de
 * errores por capas de ADR-031.
 * <p>
 * Los tres puertos de errores son beans con {@code @ConditionalOnMissingBean}: un servicio, o el starter
 * de una organización como UTP, declara el suyo y este lo usa. Lo que no se reemplaza es el núcleo, el
 * {@link GlobalExceptionHandler}, que registra el error una sola vez y les pasa a los puertos un fallo
 * sin el proveedor ni la causa.
 * <p>
 * Se puede desactivar con {@code nova.api-standard.enabled=false}.
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
     * El status de cada capa y tipo, con la tabla de ADR-031.
     *
     * @return el mapeador de Nova
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorStatusMapper novaErrorStatusMapper() {
        return new NovaErrorStatusMapper();
    }

    /**
     * El código y el mensaje que ve el cliente, con el catálogo de la plataforma.
     *
     * @return el catálogo de Nova
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorCatalog novaErrorCatalog() {
        return new NovaErrorCatalog();
    }

    /**
     * El cuerpo y los headers de un error: el sobre de Nova, con {@code metadata.traceId} y
     * {@code Retry-After}.
     *
     * @return el serializador de Nova
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorSerializer novaErrorSerializer() {
        return new NovaErrorSerializer();
    }

    /**
     * Los tres puertos juntos, en el orden en que se consultan.
     *
     * @param statusMapper el mapeador
     * @param catalog      el catálogo
     * @param serializer   el serializador
     * @return los puertos
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorPorts novaErrorPorts(ErrorStatusMapper statusMapper, ErrorCatalog catalog,
                                     ErrorSerializer serializer) {
        return new ErrorPorts(statusMapper, catalog, serializer);
    }

    /**
     * Un contador que no cuenta, si el servicio no tiene Micrometer ni uno propio.
     *
     * @return el contador vacío
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorCounter novaErrorCounter() {
        return ErrorCounter.none();
    }

    /**
     * El interceptor que envuelve las respuestas en el sobre de Nova.
     *
     * @param ports los puertos de errores
     * @return el interceptor
     */
    @Bean
    public ApiResponseInterceptor apiResponseInterceptor(ErrorPorts ports) {
        return new ApiResponseInterceptor(ports);
    }

    /**
     * El manejador de errores: el núcleo de ADR-031 en Spring MVC.
     *
     * @param ports   los puertos de errores
     * @param counter el contador de errores
     * @return el manejador
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler(ErrorPorts ports, ErrorCounter counter) {
        return new GlobalExceptionHandler(ports, counter);
    }

    /**
     * La métrica {@code nova.errors}, si el servicio tiene Micrometer. Se procesa antes que los beans de la
     * clase, así que el contador vacío solo queda si no hay Micrometer.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    static class MicrometerErrorCounterConfiguration {

        /**
         * Cuenta cada error en {@code nova.errors}, con las etiquetas {@code layer} y {@code code}, en el
         * registro de métricas del servicio. Si el servicio no registró uno, no cuenta nada.
         *
         * @param registries el registro de métricas, si hay uno
         * @return el contador
         */
        @Bean
        @ConditionalOnMissingBean
        ErrorCounter novaMicrometerErrorCounter(ObjectProvider<MeterRegistry> registries) {
            return (layer, code) -> registries.ifAvailable(registry ->
                    registry.counter("nova.errors", "layer", layer, "code", code).increment());
        }
    }
}
