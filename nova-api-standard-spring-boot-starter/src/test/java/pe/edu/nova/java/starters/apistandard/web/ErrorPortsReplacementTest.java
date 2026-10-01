package pe.edu.nova.java.starters.apistandard.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.nova.java.libs.api.standard.error.CatalogEntry;
import pe.edu.nova.java.libs.api.standard.error.ErrorCatalog;
import pe.edu.nova.java.libs.api.standard.error.ErrorSerializer;
import pe.edu.nova.java.libs.api.standard.error.SerializedError;

/**
 * Una organización, como UTP, reemplaza los puertos con sus propios beans y el starter los usa, sin forkear.
 * Ni así un puerto ve al proveedor que falló.
 */
@SpringBootTest(classes = {ApiStandardTestApplication.class, ErrorPortsReplacementTest.OrganizationPorts.class})
@AutoConfigureMockMvc
class ErrorPortsReplacementTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void theCatalogOfTheOrganizationDecidesCodesAndTexts() throws Exception {
        mvc.perform(get("/orders/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("ORG-404"))
                .andExpect(jsonPath("$.detail").value("Texto de la organización"));
    }

    @Test
    void theSerializerOfTheOrganizationReplacesTheBodyAndTheEnvelopeDoesNotWrapIt() throws Exception {
        mvc.perform(get("/orders/42/shipping"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(header().string("X-Organization", "utp"))
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(content().string(not(containsString(OrderController.UPSTREAM))));
    }

    @Test
    void aFrameworkExceptionAlsoGoesThroughThePortsOfTheOrganization() throws Exception {
        mvc.perform(get("/nada.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("ORG-404"));
    }

    /** Los puertos de una organización: su catálogo y un serializador con RFC 7807. */
    @TestConfiguration
    static class OrganizationPorts {

        @Bean
        ErrorCatalog organizationCatalog() {
            return failure -> new CatalogEntry("ORG-" + failure.status(), "Texto de la organización");
        }

        @Bean
        ErrorSerializer organizationSerializer() {
            return failure -> {
                Map<String, Object> problem = new LinkedHashMap<>();
                problem.put("status", failure.status());
                problem.put("title", failure.code());
                problem.put("detail", failure.message());
                // El fallo entero, para probar que no trae al proveedor.
                problem.put("failure", failure.toString());
                return new SerializedError(failure.status(), problem,
                        Map.of("Content-Type", "application/problem+json", "X-Organization", "utp"));
            };
        }
    }
}
