package pe.edu.nova.java.starters.apistandard.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * La suite de contrato de ADR-031 por HTTP: cada caso responde el status y el código del ADR, con
 * {@code success: false}, el {@code status} en el cuerpo y {@code metadata.traceId}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class NovaErrorContractTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MeterRegistry meters;

    @AfterEach
    void clearTheTrace() {
        MDC.clear();
    }

    @Test
    void domainNotFoundWithItsOwnCode() throws Exception {
        contract(get("/orders/42"), 404)
                .andExpect(jsonPath("$.errors[0].code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.errors[0].message").value("El pedido 42 no existe"));
    }

    @Test
    void domainConflictWithoutCode() throws Exception {
        contract(post("/orders/42/confirmations"), 409)
                .andExpect(jsonPath("$.errors[0].code").value("CONFLICT"));
    }

    @Test
    void applicationInvalidInputWithTwoFields() throws Exception {
        contract(post("/orders"), 400)
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[*].code", everyItem(is("BAD_REQUEST"))))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "quantity")));
    }

    @Test
    void applicationConflictWithRetryAfterOfOneSecond() throws Exception {
        contract(post("/orders/42/payments"), 409)
                .andExpect(jsonPath("$.errors[0].code").value("CONFLICT"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"));
    }

    @Test
    void applicationRateLimitedWithThirtySeconds() throws Exception {
        contract(get("/orders/export"), 429)
                .andExpect(jsonPath("$.errors[0].code").value("TOO_MANY_REQUESTS"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"));
    }

    @Test
    void infrastructureTimeoutNeverNamesTheUpstreamInTheBody(CapturedOutput output) throws Exception {
        contract(get("/orders/42/shipping"), 504)
                .andExpect(jsonPath("$.errors[0].code").value("GATEWAY_TIMEOUT"))
                .andExpect(jsonPath("$.errors[0].message").value("Una dependencia no respondió a tiempo"))
                .andExpect(content().string(not(containsString(OrderController.UPSTREAM))));

        // El proveedor sí va al log, en error y con la causa.
        assertThat(output)
                .containsPattern("ERROR .*\\[Nova Platform\\] 504 GATEWAY_TIMEOUT layer=infrastructure upstream="
                        + OrderController.UPSTREAM)
                .contains("\tat ");
    }

    @Test
    void infrastructureUnavailable() throws Exception {
        contract(get("/orders/42/stock"), 503)
                .andExpect(jsonPath("$.errors[0].code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void anyOtherExceptionIsAPlatformError() throws Exception {
        contract(get("/items/1/audit"), 500)
                .andExpect(jsonPath("$.errors[0].code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errors[0].message").value("Error interno del servidor"));
    }

    @Test
    void theTraceIdOfTheRequestReachesTheBodyAndTheLog(CapturedOutput output) throws Exception {
        MDC.put(MdcTraceIdSource.TRACE_ID_KEY, TRACE_ID);

        mvc.perform(get("/orders/42"))
                .andExpect(jsonPath("$.metadata.traceId").value(TRACE_ID));

        assertThat(output).contains("traceId=" + TRACE_ID);
    }

    @Test
    void withoutATraceIdTheSameGeneratedOneGoesToTheBodyAndTheLog(CapturedOutput output) throws Exception {
        String body = mvc.perform(get("/orders/42/stock")).andReturn().getResponse().getContentAsString();

        String traceId = JsonPath.read(body, "$.metadata.traceId");
        assertThat(traceId).isNotBlank();
        assertThat(output).contains("traceId=" + traceId);
        assertThat(MDC.get(MdcTraceIdSource.TRACE_ID_KEY)).as("the generated id leaves the MDC").isNull();
    }

    @Test
    void anExpectedErrorIsAWarningWithoutStackTrace(CapturedOutput output) throws Exception {
        mvc.perform(get("/orders/42"));

        assertThat(output)
                .containsPattern("WARN .*\\[Nova Platform\\] 404 ORDER_NOT_FOUND layer=domain")
                .doesNotContain("\tat ");
    }

    @Test
    void eachErrorIsCountedByLayerAndCode() throws Exception {
        double before = meters.counter("nova.errors", "layer", "domain", "code", "ORDER_NOT_FOUND").count();

        mvc.perform(get("/orders/42"));

        assertThat(meters.counter("nova.errors", "layer", "domain", "code", "ORDER_NOT_FOUND").count())
                .isEqualTo(before + 1);
    }

    private ResultActions contract(
            org.springframework.test.web.servlet.RequestBuilder request, int status) throws Exception {
        return mvc.perform(request)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.metadata.traceId").value(notNullValue()))
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
