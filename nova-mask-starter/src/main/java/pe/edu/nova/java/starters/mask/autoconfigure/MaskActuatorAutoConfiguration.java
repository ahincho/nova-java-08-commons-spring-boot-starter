package pe.edu.nova.java.starters.mask.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import pe.edu.nova.java.starters.mask.actuator.MaskHealthIndicator;
import pe.edu.nova.java.starters.mask.actuator.MaskInfoContributor;
import pe.edu.nova.java.starters.mask.config.MaskProperties;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;

/**
 * Auto-configuración de la integración con Spring Boot Actuator.
 * <p>
 * Registra un {@link MaskHealthIndicator} y un {@link MaskInfoContributor}
 * solo cuando Actuator está en el classpath.
 * </p>
 *
 * @author Nova Platform
 */
@AutoConfiguration(after = MaskAutoConfiguration.class)
@ConditionalOnProperty(
        prefix = "nova.mask",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
public class MaskActuatorAutoConfiguration {

    /** Crea una nueva instancia de la auto-configuración de Actuator. */
    public MaskActuatorAutoConfiguration() {
    }

    /**
     * Registra el indicador de salud del subsistema de enmascaramiento.
     *
     * @param strategyRegistry registro de estrategias
     * @return indicador de salud
     */
    @Bean
    @ConditionalOnMissingBean
    public MaskHealthIndicator maskHealthIndicator(StrategyRegistry strategyRegistry) {
        return new MaskHealthIndicator(strategyRegistry);
    }

    /**
     * Registra el contribuidor de información del subsistema de enmascaramiento.
     *
     * @param strategyRegistry registro de estrategias
     * @param properties       propiedades de configuración
     * @return contribuidor de información
     */
    @Bean
    @ConditionalOnMissingBean
    public MaskInfoContributor maskInfoContributor(
            StrategyRegistry strategyRegistry,
            MaskProperties properties) {
        return new MaskInfoContributor(strategyRegistry, properties);
    }
}
