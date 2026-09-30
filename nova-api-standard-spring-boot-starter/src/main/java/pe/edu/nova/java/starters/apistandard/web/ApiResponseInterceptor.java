package pe.edu.nova.java.starters.apistandard.web;

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * Interceptor que envuelve automáticamente las respuestas de controladores
 * REST en {@link ApiResponse}.
 * <p>
 * El sobre lleva el status real de la respuesta: el de {@code ResponseEntity},
 * el de {@code @ResponseStatus} o el que fijó el controlador, así que un 201
 * dice 201 y no 200. Un 4xx o 5xx que el controlador contesta sin lanzar una
 * excepción, como {@code ResponseEntity.notFound().build()}, sale como sobre
 * de error.
 * Si la respuesta ya es un {@code ApiResponse}, no la envuelve de nuevo.
 * Si la respuesta es {@code null} con un 204 real, retorna {@code ApiResponse.noContent()}.
 * Si la respuesta es un {@code String}, no la envuelve (Spring MVC maneja
 * strings de forma diferente con StringHttpMessageConverter).
 * </p>
 *
 * @author Nova Platform
 */
@RestControllerAdvice
public class ApiResponseInterceptor implements ResponseBodyAdvice<Object> {

    /** Crea una nueva instancia del interceptor. */
    public ApiResponseInterceptor() {
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                   MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request,
                                   ServerHttpResponse response) {
        // Si ya es ApiResponse, no envolver de nuevo
        if (body instanceof ApiResponse<?>) {
            return body;
        }

        // No envolver String (Spring MVC maneja strings de forma diferente)
        if (body instanceof String) {
            return body;
        }

        int status = statusOf(response);

        // Un 4xx o 5xx que el controlador contesta sin lanzar una excepción también es un error
        if (status >= 400) {
            return errorEnvelope(status, body);
        }

        // Si es null y el status es un 204 real, retornar noContent
        if (body == null && status == HttpStatus.NO_CONTENT.value()) {
            return ApiResponse.noContent();
        }

        // Envolver con el status real de la respuesta. El builder marca success cuando no hay
        // errores, así que con 200 el resultado es el mismo que el de ApiResponse.ok(body).
        return ApiResponse.<Object>builder().data(body).status(status).build();
    }

    /**
     * Arma el sobre de error de un 4xx o 5xx, con un error como los de
     * {@link GlobalExceptionHandler}. Si el controlador mandó un cuerpo propio, se conserva en
     * {@code data} para no perderlo.
     *
     * @param status el status HTTP de la respuesta
     * @param body el cuerpo del controlador, o {@code null} si no mandó ninguno
     * @return el sobre de error
     */
    private static ApiResponse<Object> errorEnvelope(int status, Object body) {
        ApiResponse<Object> error = ApiResponse.error(status, GlobalExceptionHandler.defaultMessage(status));
        if (body == null) {
            return error;
        }
        return new ApiResponse<>(false, status, body, error.errors(), null, List.of(), null, null);
    }

    /**
     * Lee el status que ya tiene la respuesta. Spring MVC lo fija antes de escribir el cuerpo,
     * tanto para un {@code ResponseEntity} como para un {@code @ResponseStatus}.
     *
     * @param response la respuesta que se está escribiendo
     * @return el status HTTP, o 200 si la respuesta no es de un servlet
     */
    private static int statusOf(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            return servletResponse.getServletResponse().getStatus();
        }
        return HttpStatus.OK.value();
    }
}
