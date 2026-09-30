package pe.edu.nova.java.starters.apistandard.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Las excepciones propias de Spring MVC salen con su status y su código, no como 500 ni como ERROR. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SpringExceptionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void aMissingHeaderIsABadRequestThatNamesTheHeader() throws Exception {
        mvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Silla\",\"quantity\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString(ItemController.TENANT_HEADER)));
    }

    @Test
    void anUnreadableBodyIsABadRequest() throws Exception {
        mvc.perform(createItem("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors[0].message").value("No se pudo leer el cuerpo de la solicitud"));
    }

    @Test
    void anInvalidBodyIsABadRequestWithOneErrorPerField() throws Exception {
        mvc.perform(createItem("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[*].code", everyItem(is("VALIDATION_ERROR"))))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "quantity")))
                .andExpect(jsonPath("$.errors[*].message",
                        containsInAnyOrder("El nombre es obligatorio", "La cantidad debe ser positiva")));
    }

    @Test
    void anInvalidParameterIsABadRequestThatNamesTheParameter() throws Exception {
        mvc.perform(get("/items/top").param("limit", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("limit"))
                .andExpect(jsonPath("$.errors[0].message").value("No se pueden pedir más de 10"));
    }

    @Test
    void aValueOfTheWrongTypeIsABadRequest() throws Exception {
        mvc.perform(get("/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("'id'")));
    }

    @Test
    void aMissingParameterIsABadRequest() throws Exception {
        mvc.perform(get("/items/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("'name'")));
    }

    @Test
    void anUnsupportedMethodIsMethodNotAllowedWithTheAllowedOnes() throws Exception {
        mvc.perform(put("/items"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.errors[0].code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void anUnsupportedContentTypeIsUnsupportedMediaType() throws Exception {
        mvc.perform(post("/items")
                        .header(ItemController.TENANT_HEADER, "utp")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Silla"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.errors[0].code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void aResponseStatusExceptionKeepsItsStatusAndReason() throws Exception {
        mvc.perform(post("/items/1/reservations"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errors[0].code").value("CONFLICT"))
                .andExpect(jsonPath("$.errors[0].message").value("El ítem ya está reservado"));
    }

    @Test
    void aServerErrorFromSpringKeepsItsStatusButHidesItsReason() throws Exception {
        mvc.perform(get("/items/1/price"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.errors[0].message").value("Error interno del servidor"))
                .andExpect(content().string(not(containsString("Acme"))));
    }

    @Test
    void anIllegalArgumentIsStillABadRequestWithItsMessage() throws Exception {
        mvc.perform(get("/items/1/check"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors[0].message").value("El ítem no es válido"));
    }

    @Test
    void anIllegalArgumentWithoutMessageIsABadRequestWithTheStandardPhrase() throws Exception {
        mvc.perform(get("/items/1/verify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors[0].message").value("Bad Request"));
    }

    @Test
    void anUnexpectedExceptionIsStillAGenericServerError() throws Exception {
        mvc.perform(get("/items/1/audit"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errors[0].message").value("Error interno del servidor"))
                .andExpect(content().string(not(containsString("Acme"))));
    }

    @Test
    void aMissingStaticResourceKeepsItsMessage() throws Exception {
        mvc.perform(get("/nada.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.errors[0].message").value("Recurso no encontrado: nada.txt"));
    }

    @Test
    void aClientErrorIsLoggedAsAWarningWithoutStackTrace(CapturedOutput output) throws Exception {
        mvc.perform(get("/items/search")).andExpect(status().isBadRequest());

        assertThat(output).containsPattern("WARN .*\\[Nova Platform\\].*400").doesNotContain("\tat ");
    }

    @Test
    void aServerErrorIsLoggedAsAnErrorWithItsCause(CapturedOutput output) throws Exception {
        mvc.perform(get("/items/1/price")).andExpect(status().isServiceUnavailable());

        assertThat(output)
                .containsPattern("ERROR .*\\[Nova Platform\\].*503")
                .contains("El servicio de precios de Acme no responde")
                .contains("\tat ");
    }

    private static MockHttpServletRequestBuilder createItem(String body) {
        return post("/items")
                .header(ItemController.TENANT_HEADER, "utp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
