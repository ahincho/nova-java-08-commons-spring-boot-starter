package pe.edu.nova.java.starters.mask.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;

import pe.edu.nova.java.starters.mask.config.MaskProperties;
import pe.edu.nova.java.starters.mask.strategy.MaskStrategyBean;
import pe.edu.nova.java.libs.mask.utils.CountryCode;
import pe.edu.nova.java.libs.mask.utils.strategy.MaskStrategy;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;

/**
 * Auto-configuración principal del starter mask-utils.
 * <p>
 * Registra el {@link StrategyRegistry} con las estrategias predeterminadas
 * y detecta automáticamente beans {@link MaskStrategy} anotados con
 * {@link MaskStrategyBean} para registrarlos en el registry.
 * </p>
 *
 * @author Nova Platform
 */
@AutoConfiguration
@EnableConfigurationProperties(MaskProperties.class)
@ConditionalOnProperty(
        prefix = "nova.mask",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MaskAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MaskAutoConfiguration.class);

    /** Crea una nueva instancia de la auto-configuración principal. */
    public MaskAutoConfiguration() {
    }

    /**
     * Registra el {@link StrategyRegistry} con estrategias predeterminadas
     * y estrategias personalizadas detectadas en el contexto de Spring.
     *
     * @param properties       propiedades de configuración del starter
     * @param customStrategies proveedor de estrategias personalizadas
     * @return registry configurado con todas las estrategias
     */
    @Bean
    @ConditionalOnMissingBean
    public StrategyRegistry strategyRegistry(
            MaskProperties properties,
            ObjectProvider<MaskStrategy> customStrategies) {

        StrategyRegistry registry = StrategyRegistry.getDefault();

        customStrategies.orderedStream().forEach(strategy -> {
            MaskStrategyBean annotation = AnnotationUtils.findAnnotation(
                    strategy.getClass(), MaskStrategyBean.class);

            if (annotation != null) {
                CountryCode country = CountryCode.fromCode(annotation.country());
                registry.register(annotation.type(), country, strategy);
                log.info("Estrategia personalizada registrada: tipo={}, país={}, clase={}",
                        annotation.type(), country, strategy.getClass().getName());
            } else {
                log.warn("El bean MaskStrategy '{}' no tiene la anotación @MaskStrategyBean; "
                                + "no se puede registrar automáticamente en el StrategyRegistry.",
                        strategy.getClass().getName());
            }
        });

        return registry;
    }
}
