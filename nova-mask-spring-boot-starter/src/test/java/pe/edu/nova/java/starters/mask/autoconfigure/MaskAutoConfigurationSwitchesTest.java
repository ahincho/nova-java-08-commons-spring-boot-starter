package pe.edu.nova.java.starters.mask.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import pe.edu.nova.java.libs.mask.utils.strategy.StrategyRegistry;
import pe.edu.nova.java.starters.mask.actuator.MaskHealthIndicator;
import pe.edu.nova.java.starters.mask.actuator.MaskInfoContributor;
import pe.edu.nova.java.starters.mask.config.MaskProperties;
import pe.edu.nova.java.starters.mask.log.MaskingLogbackLayout;
import pe.edu.nova.java.starters.mask.web.MaskResponseBodyAdvice;

/**
 * Los interruptores del starter: {@code nova.mask.enabled=false} lo apaga entero, y cada pieza
 * tiene el suyo. Apagarlo no puede romper el arranque: un servicio que lo apaga es uno que no
 * quiere nada de lo que hace.
 */
class MaskAutoConfigurationSwitchesTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    MaskAutoConfiguration.class,
                    MaskJacksonAutoConfiguration.class,
                    MaskWebAutoConfiguration.class,
                    MaskLogAutoConfiguration.class,
                    MaskActuatorAutoConfiguration.class));

    @Test
    void aServiceWithoutConfigurationGetsEveryPieceOfTheStarter() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(MaskProperties.class)
                .hasSingleBean(StrategyRegistry.class)
                .hasSingleBean(MaskResponseBodyAdvice.class)
                .hasSingleBean(MaskingLogbackLayout.class)
                .hasSingleBean(MaskHealthIndicator.class)
                .hasSingleBean(MaskInfoContributor.class));
    }

    @Test
    void withTheStarterDisabledTheServiceStartsAndTheStarterRegistersNothing() {
        runner.withPropertyValues("nova.mask.enabled=false").run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(MaskProperties.class)
                .doesNotHaveBean(StrategyRegistry.class)
                .doesNotHaveBean(MaskResponseBodyAdvice.class)
                .doesNotHaveBean(MaskingLogbackLayout.class)
                .doesNotHaveBean(MaskHealthIndicator.class)
                .doesNotHaveBean(MaskInfoContributor.class));
    }

    @Test
    void theLogLayoutCanBeTurnedOffAlone() {
        runner.withPropertyValues("nova.mask.log.enabled=false").run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(MaskingLogbackLayout.class)
                .hasSingleBean(StrategyRegistry.class)
                .hasSingleBean(MaskResponseBodyAdvice.class));
    }

    @Test
    void theResponseAdviceCanBeTurnedOffAlone() {
        runner.withPropertyValues("nova.mask.response.enabled=false").run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(MaskResponseBodyAdvice.class)
                .hasSingleBean(StrategyRegistry.class)
                .hasSingleBean(MaskingLogbackLayout.class));
    }
}
