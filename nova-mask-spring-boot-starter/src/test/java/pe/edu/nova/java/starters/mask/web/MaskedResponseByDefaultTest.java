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
 * Lo que contesta un servicio con el starter y sin configurarlo: solo se enmascara lo que lleva
 * {@code @Masked} o {@code @MaskedClass}. Un {@code name} o un {@code email} sin anotación se
 * contesta tal cual, porque la respuesta es un dato del servicio, no un log.
 */
@SpringBootTest(classes = MaskTestApplication.class, properties = "nova.mask.default-country=PE")
@AutoConfigureMockMvc
class MaskedResponseByDefaultTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void aProductNameIsNotMaskedInTheResponse() throws Exception {
        mvc.perform(get("/catalog/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Taza"))
                .andExpect(jsonPath("$.category").value("Hogar"));
    }

    @Test
    void aDtoWithoutAnnotationsIsAnsweredInTheClear() throws Exception {
        mvc.perform(get("/catalog/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ceramicas del Sur"))
                .andExpect(jsonPath("$.email").value("ventas@ceramicasdelsur.pe"))
                .andExpect(jsonPath("$.phone").value("+51 987 654 321"));
    }

    @Test
    void theFieldsThatCarryMaskedAreMaskedInTheResponse() throws Exception {
        mvc.perform(get("/catalog/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Juan Perez"))
                .andExpect(jsonPath("$.email").value("j*********@acme.pe"))
                .andExpect(jsonPath("$.phone").value("+51 *** *** 321"));
    }

    @Test
    void aMaskedClassIsMaskedInTheResponse() throws Exception {
        mvc.perform(get("/catalog/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("J*** P****"))
                .andExpect(jsonPath("$.email").value("j*********@acme.pe"))
                .andExpect(jsonPath("$.diagnosis").value("Gripe estacional"));
    }

    @Test
    void aSkipMaskingFieldOfAMaskedClassStaysInTheClear() throws Exception {
        mvc.perform(get("/catalog/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("J*** P****"))
                .andExpect(jsonPath("$.email").value("juan.perez@acme.pe"));
    }
}
