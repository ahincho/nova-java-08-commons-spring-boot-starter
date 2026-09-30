package pe.edu.nova.java.starters.apistandard.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Cada status lleva el código de su fila del catálogo de la plataforma, y ninguno es el genérico ERROR. */
class ErrorCodesTest {

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource(textBlock = """
            400, BAD_REQUEST
            401, UNAUTHORIZED
            403, FORBIDDEN
            404, NOT_FOUND
            405, METHOD_NOT_ALLOWED
            406, NOT_ACCEPTABLE
            408, REQUEST_TIMEOUT
            409, CONFLICT
            410, GONE
            415, UNSUPPORTED_MEDIA_TYPE
            422, UNPROCESSABLE_ENTITY
            429, TOO_MANY_REQUESTS
            500, INTERNAL_SERVER_ERROR
            502, BAD_GATEWAY
            503, SERVICE_UNAVAILABLE
            504, GATEWAY_TIMEOUT
            """)
    void aStatusOfTheCatalogHasItsOwnCode(int status, String code) {
        assertThat(ErrorCodes.of(status)).isEqualTo(code);
    }

    @ParameterizedTest
    @ValueSource(ints = {402, 407, 412, 418, 423, 451, 499})
    void aClientErrorTheCatalogDoesNotNameIsARequestError(int status) {
        assertThat(ErrorCodes.of(status)).isEqualTo("REQUEST_ERROR");
    }

    @ParameterizedTest
    @ValueSource(ints = {501, 505, 507, 511, 599})
    void aServerErrorTheCatalogDoesNotNameIsAnInternalServerError(int status) {
        assertThat(ErrorCodes.of(status)).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
