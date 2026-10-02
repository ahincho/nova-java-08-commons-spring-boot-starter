package pe.edu.nova.java.starters.mask.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import pe.edu.nova.java.starters.mask.web.MaskResponseBodyAdvice;

/**
 * Auto-configuración del enmascaramiento de respuestas REST.
 * <p>
 * Registra un {@link MaskResponseBodyAdvice} que intercepta las respuestas
 * de controladores {@code @RestController} para habilitar el enmascaramiento
 * automático de campos {@code @Masked} vía Jackson.
 * </p>
 * <p>
 * Se apaga con {@code nova.mask.enabled=false}, como todo el starter;
 * {@code nova.mask.response.enabled=false} apaga solo el advice.
 * </p>
 *
 * @author Nova Platform
 */
@AutoConfiguration(after = MaskJacksonAutoConfiguration.class)
@ConditionalOnProperty(
        prefix = "nova.mask",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MaskWebAutoConfiguration {

    /** Crea una nueva instancia de la auto-configuración web. */
    public MaskWebAutoConfiguration() {
    }

    /**
     * Registra el advice de enmascaramiento de respuestas REST.
     *
     * @return advice de enmascaramiento
     */
    @Bean
    @ConditionalOnMissingBean(MaskResponseBodyAdvice.class)
    @ConditionalOnProperty(
            prefix = "nova.mask.response",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public MaskResponseBodyAdvice maskResponseBodyAdvice() {
        return new MaskResponseBodyAdvice();
    }
}
