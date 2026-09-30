package pe.edu.nova.java.starters.apistandard.web;

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
 * dice 201 y no 200.
 * Si la respuesta ya es un {@code ApiResponse}, no la envuelve de nuevo.
 * Si la respuesta es {@code null}, retorna {@code ApiResponse.noContent()}.
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

        // Si es null, retornar noContent
        if (body == null) {
            return ApiResponse.noContent();
        }

        // Envolver con el status real de la respuesta. El builder marca success cuando no hay
        // errores, así que con 200 el resultado es el mismo que el de ApiResponse.ok(body).
        return ApiResponse.<Object>builder().data(body).status(statusOf(response)).build();
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
