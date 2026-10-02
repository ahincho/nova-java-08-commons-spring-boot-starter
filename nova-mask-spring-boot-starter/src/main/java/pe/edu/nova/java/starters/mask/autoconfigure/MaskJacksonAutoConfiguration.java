package pe.edu.nova.java.starters.mask.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import pe.edu.nova.java.starters.mask.config.MaskProperties;
import pe.edu.nova.java.starters.mask.jackson.MaskedBeanSerializerModifier;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;
import tools.jackson.databind.module.SimpleModule;

/**
 * Auto-configuración de la integración con Jackson 3 para enmascaramiento
 * de campos {@code @Masked} durante la serialización JSON.
 * <p>
 * El módulo alcanza todo JSON que serializa el servicio, no solo sus respuestas, así que por
 * defecto enmascara únicamente lo que lleva {@code @Masked} o {@code @MaskedClass}. Enmascarar por
 * el nombre del campo es opt-in, con {@code nova.mask.infer-by-field-name=true}.
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
@ConditionalOnClass(name = "tools.jackson.databind.ObjectMapper")
public class MaskJacksonAutoConfiguration {

    /** Crea una nueva instancia de la auto-configuración de Jackson. */
    public MaskJacksonAutoConfiguration() {
    }

    /**
     * Registra el {@link MaskedBeanSerializerModifier} como módulo de Jackson.
     *
     * @param strategyRegistry registro de estrategias
     * @param properties       propiedades de configuración
     * @return customizer que agrega el módulo de enmascaramiento al ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean(MaskedBeanSerializerModifier.class)
    public JsonMapperBuilderCustomizer maskJsonMapperCustomizer(
            StrategyRegistry strategyRegistry,
            MaskProperties properties) {
        return builder -> {
            SimpleModule module = new SimpleModule("mask-utils-module");
            module.setSerializerModifier(
                    new MaskedBeanSerializerModifier(strategyRegistry, properties));
            builder.addModule(module);
        };
    }
}
