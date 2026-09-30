package pe.edu.nova.java.starters.apistandard.web;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** El sobre dice el status con el que contestó el controlador, no siempre 200. */
@SpringBootTest
@AutoConfigureMockMvc
class EnvelopeStatusTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void aCreatedResponseEntitySaysCreatedInTheEnvelope() throws Exception {
        mvc.perform(post("/items")
                        .header(ItemController.TENANT_HEADER, "utp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Silla\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("Silla"));
    }

    @Test
    void aResponseStatusAnnotationSaysItsStatusInTheEnvelope() throws Exception {
        mvc.perform(post("/items/1/copies"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(3));
    }

    @Test
    void anOkResponseStillSaysOk() throws Exception {
        mvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].name").value("Mesa"));
    }

    @Test
    void anOkWithoutBodySaysOkAndNotNoContent() throws Exception {
        mvc.perform(post("/items/1/confirmations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }
}
