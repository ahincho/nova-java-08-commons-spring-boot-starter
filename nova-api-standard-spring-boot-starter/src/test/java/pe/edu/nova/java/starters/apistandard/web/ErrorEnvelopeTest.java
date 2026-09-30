package pe.edu.nova.java.starters.apistandard.web;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Un 4xx o 5xx que contesta el controlador, con o sin cuerpo, sale como sobre de error. */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorEnvelopeTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void aNotFoundWithoutBodyIsAnErrorEnvelope() throws Exception {
        mvc.perform(get("/items/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.errors[0].message").value("Not Found"));
    }

    @Test
    void aServerErrorWithoutBodyCarriesTheGenericMessage() throws Exception {
        mvc.perform(get("/items/1/stock"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.errors[0].message").value("Error interno del servidor"));
    }

    @Test
    void aClientErrorWithABodyKeepsTheBodyAsData() throws Exception {
        mvc.perform(put("/items/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.data.reason").value("El ítem está en un pedido abierto"))
                .andExpect(jsonPath("$.errors[0].code").value("CONFLICT"));
    }

    @Test
    void aRealNoContentKeepsItsEnvelope() throws Exception {
        mvc.perform(delete("/items/1"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(204));
    }
}
