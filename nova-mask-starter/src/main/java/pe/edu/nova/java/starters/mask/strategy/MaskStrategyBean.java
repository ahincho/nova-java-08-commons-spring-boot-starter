package pe.edu.nova.java.starters.mask.strategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

import pe.edu.nova.java.libs.mask.utils.MaskType;

/**
 * Marca una implementación de {@code MaskStrategy} como bean de Spring
 * y especifica el tipo de enmascaramiento y país asociados.
 * <p>
 * El starter detecta automáticamente los beans anotados con esta anotación
 * y los registra en el {@code StrategyRegistry}.
 * </p>
 *
 * <pre>{@code
 * @MaskStrategyBean(type = MaskType.IDENTITY_DOCUMENT, country = "CO")
 * public class ColombiaIdentityMaskStrategy implements MaskStrategy {
 *     // ...
 * }
 * }</pre>
 *
 * @author Nova Platform
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MaskStrategyBean {

    /**
     * Tipo de enmascaramiento que implementa esta estrategia.
     *
     * @return tipo de enmascaramiento
     */
    MaskType type();

    /**
     * Código de país ISO 3166-1 alpha-2.
     * Usar {@code "GENERIC"} para estrategia genérica (fallback).
     *
     * @return código de país, o {@code "GENERIC"} para fallback
     */
    String country() default "GENERIC";
}
