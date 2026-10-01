package pe.edu.nova.java.starters.apistandard.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.context.annotation.ImportRuntimeHints;

import pe.edu.nova.java.libs.api.standard.error.ApiError;
import pe.edu.nova.java.libs.api.standard.link.ApiLink;
import pe.edu.nova.java.libs.api.standard.metadata.ApiMetadata;
import pe.edu.nova.java.libs.api.standard.page.PageInfo;
import pe.edu.nova.java.libs.api.standard.ratelimit.RateLimitInfo;
import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * En una imagen nativa Jackson solo lee los componentes de un record si tienen pistas de reflexión. Sin ellas
 * cada respuesta del sobre termina en un 500 con {@code Record components not available}.
 */
class ApiStandardRuntimeHintsTest {

    private final RuntimeHints hints = new RuntimeHints();

    @Test
    void theAutoConfigurationImportsTheHints() {
        assertThat(ApiStandardAutoConfiguration.class.getAnnotation(ImportRuntimeHints.class).value())
                .containsExactly(ApiStandardRuntimeHints.class);
    }

    @ParameterizedTest
    @ValueSource(classes = {
            ApiResponse.class, ApiError.class, ApiMetadata.class, ApiLink.class, RateLimitInfo.class, PageInfo.class
    })
    void everyRecordOfTheEnvelopeCanBeReadByJackson(Class<?> envelopeRecord) {
        new ApiStandardRuntimeHints().registerHints(hints, getClass().getClassLoader());

        List<String> accessors = List.of(envelopeRecord.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .toList();
        assertThat(accessors).isNotEmpty().allSatisfy(accessor -> assertThat(
                RuntimeHintsPredicates.reflection().onMethodInvocation(envelopeRecord, accessor))
                .as("%s.%s()", envelopeRecord.getSimpleName(), accessor)
                .accepts(hints));
    }
}
