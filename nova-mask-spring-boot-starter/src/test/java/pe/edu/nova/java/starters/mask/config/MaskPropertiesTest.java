package pe.edu.nova.java.starters.mask.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pe.edu.nova.java.starters.mask.autoconfigure.MaskAutoConfiguration;

/**
 * Las propiedades {@code nova.mask.*}. Inferir el enmascaramiento por el nombre del campo está
 * apagado por defecto: un starter que viene en el meta-starter no puede cambiar las respuestas de un
 * servicio sin que este se lo pida.
 */
class MaskPropertiesTest {

    private static final String METADATA = "META-INF/spring-configuration-metadata.json";

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MaskAutoConfiguration.class));

    @Test
    void inferenceByFieldNameIsOffByDefault() {
        assertThat(new MaskProperties().isInferByFieldName()).isFalse();
    }

    @Test
    void aServiceWithoutConfigurationDoesNotInferByFieldName() {
        runner.run(context -> assertThat(context.getBean(MaskProperties.class).isInferByFieldName())
                .isFalse());
    }

    @Test
    void theServiceTurnsInferenceOnWithTheKebabCaseProperty() {
        runner.withPropertyValues("nova.mask.infer-by-field-name=true")
                .run(context -> assertThat(context.getBean(MaskProperties.class).isInferByFieldName())
                        .isTrue());
    }

    @Test
    void theMetadataDocumentsThePropertyAndItsDefault() throws IOException {
        List<Map<String, Object>> found = JsonPath.read(
                novaMaskMetadata(), "$.properties[?(@.name=='nova.mask.infer-by-field-name')]");

        assertThat(found).hasSize(1);
        Map<String, Object> property = found.get(0);
        assertThat(property).containsEntry("type", "java.lang.Boolean").containsEntry("defaultValue", false);
        assertThat((String) property.get("description")).contains("@Masked").contains("@MaskedClass");
    }

    /** El metadato que el procesador de configuración genera para este starter, y no el de otro jar. */
    private static String novaMaskMetadata() throws IOException {
        List<URL> candidates = Collections.list(
                MaskPropertiesTest.class.getClassLoader().getResources(METADATA));
        for (URL url : candidates) {
            try (var in = url.openStream()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (json.contains("\"nova.mask.enabled\"")) {
                    return json;
                }
            }
        }
        throw new AssertionError("No hay metadato de configuración de nova.mask en " + candidates);
    }
}
