package pe.edu.nova.java.starters.mask.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Con {@code nova.mask.enabled=false} el servicio arranca y el starter no enmascara nada, ni
 * siquiera lo que lleva {@code @Masked} o {@code @MaskedClass}.
 */
@SpringBootTest(classes = MaskTestApplication.class, properties = "nova.mask.enabled=false")
@AutoConfigureMockMvc
class MaskedResponseDisabledTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void nothingIsMaskedNotEvenWhatCarriesMasked() throws Exception {
        mvc.perform(get("/catalog/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Juan Perez"))
                .andExpect(jsonPath("$.email").value("juan.perez@acme.pe"))
                .andExpect(jsonPath("$.phone").value("+51 987 654 321"));
    }

    @Test
    void aMaskedClassIsNotMaskedEither() throws Exception {
        mvc.perform(get("/catalog/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Juan Perez"))
                .andExpect(jsonPath("$.email").value("juan.perez@acme.pe"));
    }
}
