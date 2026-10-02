package pe.edu.nova.java.starters.mask.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pe.edu.nova.java.starters.mask.autoconfigure.MaskActuatorAutoConfiguration;
import pe.edu.nova.java.starters.mask.autoconfigure.MaskAutoConfiguration;

/**
 * Lo que {@code /actuator/info} dice de la configuración del starter. Quien opera el servicio tiene
 * que poder ver, sin leer el código, si enmascara por el nombre de los campos.
 */
class MaskInfoContributorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MaskAutoConfiguration.class, MaskActuatorAutoConfiguration.class))
            .withPropertyValues("nova.mask.default-country=PE");

    @Test
    void itReportsThatInferenceByFieldNameIsOffByDefault() {
        runner.run(context -> assertThat(maskInfo(context.getBean(MaskInfoContributor.class)))
                .containsEntry("enabled", true)
                .containsEntry("inferByFieldName", false));
    }

    @Test
    void itReportsThatInferenceByFieldNameIsOnWhenTheServiceAsksForIt() {
        runner.withPropertyValues("nova.mask.infer-by-field-name=true")
                .run(context -> assertThat(maskInfo(context.getBean(MaskInfoContributor.class)))
                        .containsEntry("inferByFieldName", true));
    }

    @Test
    void itKeepsReportingTheRestOfTheConfiguration() {
        runner.run(context -> assertThat(maskInfo(context.getBean(MaskInfoContributor.class)))
                .containsEntry("defaultCountry", "PE")
                .containsEntry("defaultMaskChar", "*")
                .containsEntry("logEnabled", true)
                .containsEntry("logAutoDetect", true)
                .containsEntry("responseEnabled", true)
                .containsKey("availableTypes"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> maskInfo(MaskInfoContributor contributor) {
        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);
        return (Map<String, Object>) builder.build().getDetails().get("mask");
    }
}
