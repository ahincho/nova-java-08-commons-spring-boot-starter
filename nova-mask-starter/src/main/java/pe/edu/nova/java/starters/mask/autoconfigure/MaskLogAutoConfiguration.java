package pe.edu.nova.java.starters.mask.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import pe.edu.nova.java.starters.mask.config.MaskProperties;
import pe.edu.nova.java.starters.mask.log.MaskingLogbackLayout;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;

/**
 * Auto-configuración del enmascaramiento de logs con Logback.
 * <p>
 * Registra un {@link MaskingLogbackLayout} que intercepta mensajes de log
 * y enmascara datos sensibles detectados automáticamente.
 * </p>
 *
 * @author Nova Platform
 */
@AutoConfiguration(after = MaskAutoConfiguration.class)
@ConditionalOnProperty(
        prefix = "nova.mask.log",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass(name = "ch.qos.logback.classic.PatternLayout")
public class MaskLogAutoConfiguration {

    /** Crea una nueva instancia de la auto-configuración de logs. */
    public MaskLogAutoConfiguration() {
    }

    /**
     * Registra el layout de Logback con enmascaramiento.
     *
     * @param strategyRegistry registro de estrategias
     * @param properties       propiedades de configuración
     * @return layout de Logback con enmascaramiento
     */
    @Bean
    @ConditionalOnMissingBean(MaskingLogbackLayout.class)
    public MaskingLogbackLayout maskingLogbackLayout(
            StrategyRegistry strategyRegistry,
            MaskProperties properties) {
        return new MaskingLogbackLayout(strategyRegistry, properties);
    }
}
