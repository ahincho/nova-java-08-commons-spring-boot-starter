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
 * Lo que contesta un servicio que pidió inferir por el nombre del campo con
 * {@code nova.mask.infer-by-field-name}: los nombres conocidos se enmascaran aunque no lleven
 * anotación, y lo explícito se respeta igual que sin la propiedad.
 */
@SpringBootTest(
        classes = MaskTestApplication.class,
        properties = {"nova.mask.default-country=PE", "nova.mask.infer-by-field-name=true"})
@AutoConfigureMockMvc
class MaskedResponseWithInferenceTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void aProductNameIsMaskedInTheResponse() throws Exception {
        mvc.perform(get("/catalog/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("T***"))
                .andExpect(jsonPath("$.category").value("Hogar"));
    }

    @Test
    void aDtoWithoutAnnotationsIsMaskedByTheNamesOfItsFields() throws Exception {
        mvc.perform(get("/catalog/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("C******** d** S**"))
                .andExpect(jsonPath("$.email").value("v*****@ceramicasdelsur.pe"))
                .andExpect(jsonPath("$.phone").value("+51 *** *** 321"));
    }

    @Test
    void theFieldsThatCarryMaskedKeepTheirExplicitType() throws Exception {
        mvc.perform(get("/catalog/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("J*** P****"))
                .andExpect(jsonPath("$.email").value("j*********@acme.pe"))
                .andExpect(jsonPath("$.phone").value("+51 *** *** 321"));
    }

    @Test
    void aSkipMaskingFieldStaysInTheClearEvenWithInference() throws Exception {
        mvc.perform(get("/catalog/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("J*** P****"))
                .andExpect(jsonPath("$.email").value("juan.perez@acme.pe"));
    }
}
