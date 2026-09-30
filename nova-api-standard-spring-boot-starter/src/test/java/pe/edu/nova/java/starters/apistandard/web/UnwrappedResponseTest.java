package pe.edu.nova.java.starters.apistandard.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lo que no contesta un controlador de la aplicación, o no es un cuerpo JSON, sale tal cual: Actuator
 * y el controlador de errores de Spring Boot tienen su propio formato, y un {@code byte[]}, un
 * {@code Resource} o un {@code String} los escribe su propio conversor. Lo que contesta un
 * manejador de excepciones del servicio sí se sigue envolviendo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UnwrappedResponseTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void theHealthEndpointAnswersWithItsOwnBody() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void theErrorControllerAnswersWithItsOwnBody() throws Exception {
        mvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/nada")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/nada"))
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void anExceptionHandlerOfTheServiceIsStillWrapped() throws Exception {
        mvc.perform(post("/items/1/locks"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(423))
                .andExpect(jsonPath("$.data.lockedBy").value("inventario"))
                .andExpect(jsonPath("$.errors[0].code").value("ERROR"));
    }

    @Test
    void aByteArrayIsWrittenAsIs() throws Exception {
        mvc.perform(get("/items/1/report"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(ItemController.REPORT));
    }

    @Test
    void aResourceIsWrittenAsIs() throws Exception {
        mvc.perform(get("/items/1/manual"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(ItemController.MANUAL));
    }

    @Test
    void aStringIsWrittenAsIs() throws Exception {
        mvc.perform(get("/items/1/name"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mesa"));
    }
}
