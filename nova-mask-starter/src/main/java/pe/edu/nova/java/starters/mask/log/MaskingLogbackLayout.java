package pe.edu.nova.java.starters.mask.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import pe.edu.nova.java.starters.mask.config.MaskProperties;
import pe.edu.nova.java.libs.mask.utils.CountryCode;
import pe.edu.nova.java.libs.mask.utils.log.LogMasker;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;

/**
 * Layout de Logback que enmascara datos sensibles en mensajes de log.
 * <p>
 * Extiende {@link PatternLayout} y aplica {@link LogMasker#maskAutoDetect}
 * para detectar y enmascarar emails, tarjetas de crédito, direcciones IP
 * y teléfonos con código de país.
 * </p>
 * <p>
 * Si el enmascaramiento falla, retorna el mensaje original sin propagar el error.
 * </p>
 *
 * @author Nova Platform
 */
public class MaskingLogbackLayout extends PatternLayout {

    /** Registro de estrategias de enmascaramiento. */
    private final StrategyRegistry strategyRegistry;

    /** Propiedades de configuración del starter. */
    private final MaskProperties properties;

    /**
     * Crea un nuevo layout de Logback con enmascaramiento.
     *
     * @param strategyRegistry registro de estrategias
     * @param properties       propiedades de configuración
     */
    public MaskingLogbackLayout(StrategyRegistry strategyRegistry,
                                 MaskProperties properties) {
        this.strategyRegistry = strategyRegistry;
        this.properties = properties;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        String message = super.doLayout(event);

        if (!properties.getLog().isAutoDetect()) {
            return message;
        }

        try {
            CountryCode country = CountryCode.fromCode(properties.getDefaultCountry());
            return LogMasker.maskAutoDetect(message, country, strategyRegistry);
        } catch (Exception e) {
            // No propagar errores de enmascaramiento — retornar mensaje original
            return message;
        }
    }
}
