package pe.edu.nova.java.starters.apistandard.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * Interceptor que envuelve automáticamente las respuestas de controladores
 * REST en {@link ApiResponse}.
 * <p>
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

        // Envolver en ApiResponse.ok()
        return ApiResponse.ok(body);
    }
}
