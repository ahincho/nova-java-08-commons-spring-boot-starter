package pe.edu.nova.java.starters.mask.jackson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pe.edu.nova.java.libs.mask.utils.CountryCode;
import pe.edu.nova.java.libs.mask.utils.MaskType;
import pe.edu.nova.java.libs.mask.utils.config.MaskConfig;
import pe.edu.nova.java.libs.mask.utils.result.MaskResult;
import pe.edu.nova.java.libs.mask.utils.strategy.MaskStrategy;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializador de Jackson 3 que enmascara el valor de un campo anotado con {@code @Masked}.
 * <p>
 * Reemplaza al serializador predeterminado del campo durante la serialización JSON.
 * Si el valor es {@code null} o vacío, se escribe sin enmascarar.
 * </p>
 *
 * @author Nova Platform
 */
public class MaskedFieldSerializer extends StdSerializer<Object> {

    private static final Logger log = LoggerFactory.getLogger(MaskedFieldSerializer.class);

    /** Registro de estrategias de enmascaramiento. */
    private final StrategyRegistry strategyRegistry;

    /** Tipo de enmascaramiento a aplicar. */
    private final MaskType maskType;

    /** Código de país para resolución de estrategia. */
    private final CountryCode countryCode;

    /** Carácter de máscara. */
    private final char maskChar;

    /**
     * Crea un nuevo serializador de campo enmascarado.
     *
     * @param strategyRegistry registro de estrategias
     * @param maskType         tipo de enmascaramiento
     * @param countryCode      código de país
     * @param maskChar         carácter de máscara
     */
    public MaskedFieldSerializer(StrategyRegistry strategyRegistry,
                                  MaskType maskType,
                                  CountryCode countryCode,
                                  char maskChar) {
        super(Object.class);
        this.strategyRegistry = strategyRegistry;
        this.maskType = maskType;
        this.countryCode = countryCode;
        this.maskChar = maskChar;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext provider) {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String stringValue = value.toString();
        if (stringValue.isEmpty()) {
            gen.writeString("");
            return;
        }

        try {
            MaskConfig config = MaskConfig.builder()
                    .maskChar(maskChar)
                    .country(countryCode)
                    .build();
            MaskStrategy strategy = strategyRegistry.resolve(maskType, countryCode);
            MaskResult result = strategy.mask(stringValue, config);
            gen.writeString(result.maskedValue());
        } catch (Exception e) {
            log.warn("Error al enmascarar campo con tipo={}, país={}: {}. Se escribe el valor original.",
                    maskType, countryCode, e.getMessage());
            gen.writeString(stringValue);
        }
    }
}
