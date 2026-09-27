package pe.edu.nova.java.starters.mask.jackson;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.edu.nova.java.starters.mask.config.MaskProperties;
import pe.edu.nova.java.libs.mask.utils.CountryCode;
import pe.edu.nova.java.libs.mask.utils.MaskType;
import pe.edu.nova.java.libs.mask.utils.annotation.Masked;
import pe.edu.nova.java.libs.mask.utils.annotation.MaskConfigAnnotation;
import pe.edu.nova.java.libs.mask.utils.annotation.SkipMasking;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * Modificador de serialización de Jackson 3 que enmascara automáticamente
 * campos {@code String} sensibles durante la serialización JSON.
 * <p>
 * El enmascaramiento se aplica en tres niveles de prioridad:
 * <ol>
 *   <li>Campos con {@code @Masked} explícito — usa el tipo y país de la anotación.</li>
 *   <li>Clases con {@code @MaskedClass} — enmascara todos los campos {@code String} por inferencia.</li>
 *   <li>Por defecto — enmascara cualquier campo {@code String} cuyo nombre coincida
 *       con el mapa de inferencia (email, telefono, dni, tarjeta, etc.).</li>
 * </ol>
 * <p>
 * Este comportamiento por defecto se puede desactivar con la propiedad
 * {@code nova.mask.enabled=false}.
 * </p>
 *
 * @author Nova Platform
 */
public class MaskedBeanSerializerModifier extends ValueSerializerModifier {

    /** Mapa de inferencia de MaskType por nombre de campo. */
    private static final Map<String, MaskType> FIELD_NAME_TO_TYPE = Map.ofEntries(
            Map.entry("email", MaskType.EMAIL),
            Map.entry("correo", MaskType.EMAIL),
            Map.entry("mail", MaskType.EMAIL),
            Map.entry("phone", MaskType.PHONE),
            Map.entry("telefono", MaskType.PHONE),
            Map.entry("tel", MaskType.PHONE),
            Map.entry("celular", MaskType.PHONE),
            Map.entry("dni", MaskType.IDENTITY_DOCUMENT),
            Map.entry("ssn", MaskType.IDENTITY_DOCUMENT),
            Map.entry("document", MaskType.IDENTITY_DOCUMENT),
            Map.entry("documento", MaskType.IDENTITY_DOCUMENT),
            Map.entry("passport", MaskType.IDENTITY_DOCUMENT),
            Map.entry("pasaporte", MaskType.IDENTITY_DOCUMENT),
            Map.entry("creditcard", MaskType.CREDIT_CARD),
            Map.entry("tarjeta", MaskType.CREDIT_CARD),
            Map.entry("card", MaskType.CREDIT_CARD),
            Map.entry("account", MaskType.BANK_ACCOUNT),
            Map.entry("cuenta", MaskType.BANK_ACCOUNT),
            Map.entry("iban", MaskType.BANK_ACCOUNT),
            Map.entry("cci", MaskType.BANK_ACCOUNT),
            Map.entry("name", MaskType.PERSON_NAME),
            Map.entry("nombre", MaskType.PERSON_NAME),
            Map.entry("firstname", MaskType.PERSON_NAME),
            Map.entry("lastname", MaskType.PERSON_NAME),
            Map.entry("ip", MaskType.IP_ADDRESS),
            Map.entry("ipaddress", MaskType.IP_ADDRESS)
    );

    /** Registro de estrategias de enmascaramiento. */
    private final StrategyRegistry strategyRegistry;

    /** Propiedades de configuración del starter. */
    private final MaskProperties properties;

    /**
     * Crea un nuevo modificador de serialización.
     *
     * @param strategyRegistry registro de estrategias
     * @param properties       propiedades de configuración
     */
    public MaskedBeanSerializerModifier(StrategyRegistry strategyRegistry,
                                         MaskProperties properties) {
        this.strategyRegistry = strategyRegistry;
        this.properties = properties;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription.Supplier beanDesc,
            List<BeanPropertyWriter> beanProperties) {

        // Leer anotaciones a nivel de clase
        Class<?> beanClass = beanDesc.getBeanClass();
        MaskConfigAnnotation classAnnotation = beanClass.getAnnotation(MaskConfigAnnotation.class);
        boolean skipClass = beanClass.isAnnotationPresent(SkipMasking.class);
        char classMaskChar = classAnnotation != null ? classAnnotation.maskChar() : properties.getDefaultMaskChar();
        String classCountry = classAnnotation != null && !classAnnotation.country().isEmpty()
                ? classAnnotation.country()
                : properties.getDefaultCountry();

        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter writer = beanProperties.get(i);

            // @SkipMasking en el campo — nunca enmascarar
            if (writer.getAnnotation(SkipMasking.class) != null) {
                continue;
            }

            Masked masked = writer.getAnnotation(Masked.class);

            // Prioridad 1: campo con @Masked explícito (se respeta incluso con @SkipMasking en clase)
            if (masked != null) {
                aplicarEnmascaramiento(writer, resolveMaskType(masked, writer.getName()),
                        resolveCountry(masked.country(), classCountry), classMaskChar);
                continue;
            }

            // @SkipMasking a nivel de clase — no enmascarar por inferencia
            if (skipClass) {
                continue;
            }

            // Prioridad 2: campo String cuyo nombre coincide con el mapa de inferencia
            if (isStringType(writer)) {
                String lowerName = writer.getName().toLowerCase(Locale.ROOT);
                MaskType inferido = FIELD_NAME_TO_TYPE.get(lowerName);
                if (inferido != null) {
                    aplicarEnmascaramiento(writer, inferido,
                            CountryCode.fromCode(classCountry), classMaskChar);
                }
            }
        }

        return beanProperties;
    }

    /**
     * Aplica el serializador de enmascaramiento a un campo.
     */
    private void aplicarEnmascaramiento(BeanPropertyWriter writer, MaskType maskType,
                                         CountryCode country, char maskChar) {
        @SuppressWarnings("unchecked")
        var castedSerializer = (tools.jackson.databind.ValueSerializer<Object>) (tools.jackson.databind.ValueSerializer<?>)
                new MaskedFieldSerializer(strategyRegistry, maskType, country, maskChar);
        writer.assignSerializer(castedSerializer);
    }

    /**
     * Resuelve el CountryCode desde el país del campo o el de la clase.
     */
    private CountryCode resolveCountry(String fieldCountry, String classCountry) {
        if (fieldCountry != null && !fieldCountry.isEmpty()) {
            return CountryCode.fromCode(fieldCountry);
        }
        return CountryCode.fromCode(classCountry);
    }

    /**
     * Verifica si el tipo del campo es String.
     */
    private boolean isStringType(BeanPropertyWriter writer) {
        return String.class.equals(writer.getType().getRawClass());
    }

    /**
     * Resuelve el MaskType desde la anotación {@code @Masked} o lo infiere del nombre del campo.
     *
     * @param masked    anotación del campo
     * @param fieldName nombre del campo
     * @return tipo de enmascaramiento resuelto
     */
    private MaskType resolveMaskType(Masked masked, String fieldName) {
        MaskType annotationType = masked.type();
        if (annotationType != MaskType.EMAIL) {
            return annotationType;
        }
        return inferirMaskType(fieldName);
    }

    /**
     * Infiere el MaskType desde el nombre del campo usando el mapa de inferencia.
     * Si no hay coincidencia, retorna {@link MaskType#EMAIL} como default.
     *
     * @param fieldName nombre del campo
     * @return tipo de enmascaramiento inferido
     */
    private MaskType inferirMaskType(String fieldName) {
        String lowerName = fieldName.toLowerCase(Locale.ROOT);
        MaskType inferred = FIELD_NAME_TO_TYPE.get(lowerName);
        return inferred != null ? inferred : MaskType.EMAIL;
    }
}
