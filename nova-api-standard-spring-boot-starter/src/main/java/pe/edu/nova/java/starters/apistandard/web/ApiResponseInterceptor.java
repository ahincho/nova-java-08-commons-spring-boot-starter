package pe.edu.nova.java.starters.apistandard.web;

import java.util.List;
import java.util.Objects;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJsonHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import pe.edu.nova.java.libs.api.standard.error.ErrorPorts;
import pe.edu.nova.java.libs.api.standard.error.SanitizedFailure;
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
 * strings de forma diferente con StringHttpMessageConverter), y tampoco un
 * {@code byte[]} ni un {@code Resource}.
 * </p>
 * <p>
 * Solo envuelve las respuestas de los controladores de la aplicación. Los
 * endpoints de Actuator y el controlador de errores de Spring Boot contestan
 * con su propio formato, y un health caído sigue diciendo
 * {@code "status":"DOWN"}.
 * </p>
 *
 * @author Nova Platform
 */
@RestControllerAdvice
public class ApiResponseInterceptor implements ResponseBodyAdvice<Object> {

    /** Paquete de Spring Boot: ahí viven los endpoints de Actuator y los controladores de errores. */
    private static final String SPRING_BOOT_PACKAGE = "org.springframework.boot.";

    private final ErrorPorts ports;

    /**
     * Crea el interceptor.
     *
     * @param ports los puertos de errores, con los que arma el sobre de un 4xx o 5xx sin excepción
     */
    public ApiResponseInterceptor(ErrorPorts ports) {
        this.ports = Objects.requireNonNull(ports, "ports es obligatorio");
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Lo que responde GlobalExceptionHandler ya lo armaron los puertos, aunque no sea un ApiResponse
        return isObjectConverter(converterType)
                && returnType.getContainingClass() != GlobalExceptionHandler.class
                && isApplicationHandler(returnType.getContainingClass());
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

        // No envolver String (Spring MVC maneja strings de forma diferente), ni lo que no es un
        // cuerpo JSON, como un byte[] o un Resource
        if (body instanceof String || body instanceof byte[] || body instanceof Resource) {
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
     * Indica si el conversor elegido escribe objetos, con Jackson, Gson o JSON-B. Un
     * {@code String}, un {@code byte[]} o un {@code Resource} los escribe su propio conversor, que
     * no sabe escribir un {@code ApiResponse}: envolverlos terminaba en un
     * {@code ClassCastException}.
     *
     * @param converterType el conversor que Spring MVC eligió para el cuerpo
     * @return {@code true} si el conversor puede escribir el sobre
     */
    @SuppressWarnings("removal")
    private static boolean isObjectConverter(Class<?> converterType) {
        return AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType)
                // Jackson 2 sigue disponible en Spring Boot 4 para los servicios que todavía migran
                || AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType)
                || AbstractJsonHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    /**
     * Indica si el handler es de la aplicación: un {@code @Controller} o {@code @RestController}, o
     * un {@code @ControllerAdvice} con sus {@code @ExceptionHandler}. Los endpoints de Actuator no
     * son controladores, y el controlador de errores de Spring Boot vive en el paquete de Spring
     * Boot.
     *
     * @param handlerType la clase del handler que contesta
     * @return {@code true} si la respuesta se envuelve
     */
    private static boolean isApplicationHandler(Class<?> handlerType) {
        if (handlerType.getName().startsWith(SPRING_BOOT_PACKAGE)) {
            return false;
        }
        return AnnotatedElementUtils.hasAnnotation(handlerType, Controller.class)
                || AnnotatedElementUtils.hasAnnotation(handlerType, ControllerAdvice.class);
    }

    /**
     * Arma el sobre de error de un 4xx o 5xx con los puertos, como una excepción del framework con ese
     * status. Si el controlador mandó un cuerpo propio, se conserva en {@code data} para no perderlo; si
     * el serializador del servicio no arma un {@link ApiResponse}, sale lo que él arme.
     *
     * @param status el status HTTP de la respuesta
     * @param body el cuerpo del controlador, o {@code null} si no mandó ninguno
     * @return el sobre de error
     */
    private Object errorEnvelope(int status, Object body) {
        Object serialized = ports.respond(SanitizedFailure.ofStatus(status, null, null, null, null)).body();
        if (body == null || !(serialized instanceof ApiResponse<?> error)) {
            return serialized;
        }
        return new ApiResponse<>(false, status, body, error.errors(), error.metadata(), List.of(), null, null);
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
