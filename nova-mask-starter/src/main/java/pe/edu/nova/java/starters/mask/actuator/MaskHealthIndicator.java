package pe.edu.nova.java.starters.mask.actuator;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import pe.edu.nova.java.libs.mask.utils.CountryCode;
import pe.edu.nova.java.libs.mask.utils.MaskType;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;

/**
 * Indicador de salud del subsistema de enmascaramiento para Spring Boot Actuator.
 * <p>
 * Reporta estado {@code UP} si el {@link StrategyRegistry} tiene al menos
 * una estrategia registrada, y {@code DOWN} en caso contrario.
 * </p>
 *
 * @author Nova Platform
 */
public class MaskHealthIndicator implements HealthIndicator {

    /** Registro de estrategias de enmascaramiento. */
    private final StrategyRegistry strategyRegistry;

    /**
     * Crea un nuevo indicador de salud.
     *
     * @param strategyRegistry registro de estrategias
     */
    public MaskHealthIndicator(StrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public Health health() {
        boolean hasStrategies = false;
        for (MaskType type : MaskType.values()) {
            if (strategyRegistry.hasStrategy(type, CountryCode.GENERIC)) {
                hasStrategies = true;
                break;
            }
        }

        if (hasStrategies) {
            return Health.up()
                    .withDetail("estrategias", "disponibles")
                    .build();
        }

        return Health.down()
                .withDetail("razón", "No hay estrategias de enmascaramiento registradas")
                .build();
    }
}
