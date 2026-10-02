package pe.edu.nova.java.starters.mask.actuator;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import pe.edu.nova.java.starters.mask.config.MaskProperties;
import pe.edu.nova.java.libs.mask.utils.CountryCode;
import pe.edu.nova.java.libs.mask.utils.MaskType;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;

/**
 * Contribuidor de información del subsistema de enmascaramiento para
 * el endpoint {@code /actuator/info} de Spring Boot Actuator.
 * <p>
 * Expone la configuración activa del starter, los tipos de enmascaramiento
 * disponibles y la cantidad de estrategias registradas.
 * </p>
 *
 * @author Nova Platform
 */
public class MaskInfoContributor implements InfoContributor {

    /** Registro de estrategias de enmascaramiento. */
    private final StrategyRegistry strategyRegistry;

    /** Propiedades de configuración del starter. */
    private final MaskProperties properties;

    /**
     * Crea un nuevo contribuidor de información.
     *
     * @param strategyRegistry registro de estrategias
     * @param properties       propiedades de configuración
     */
    public MaskInfoContributor(StrategyRegistry strategyRegistry, MaskProperties properties) {
        this.strategyRegistry = strategyRegistry;
        this.properties = properties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> maskInfo = new LinkedHashMap<>();
        maskInfo.put("enabled", properties.isEnabled());
        maskInfo.put("inferByFieldName", properties.isInferByFieldName());
        maskInfo.put("defaultCountry", properties.getDefaultCountry());
        maskInfo.put("defaultMaskChar", String.valueOf(properties.getDefaultMaskChar()));
        maskInfo.put("logEnabled", properties.getLog().isEnabled());
        maskInfo.put("logAutoDetect", properties.getLog().isAutoDetect());
        maskInfo.put("responseEnabled", properties.getResponse().isEnabled());
        List<String> availableTypes = Arrays.stream(MaskType.values())
                .filter(type -> strategyRegistry.hasStrategy(type, CountryCode.GENERIC))
                .map(MaskType::name)
                .toList();
        maskInfo.put("availableTypes", availableTypes);
        builder.withDetail("mask", maskInfo);
    }
}
