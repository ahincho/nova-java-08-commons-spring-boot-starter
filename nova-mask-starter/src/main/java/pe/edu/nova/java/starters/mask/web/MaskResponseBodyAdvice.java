package pe.edu.nova.java.starters.mask.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Interceptor de respuestas REST que habilita el enmascaramiento automático
 * de campos {@code @Masked} durante la serialización JSON.
 * <p>
 * El enmascaramiento real lo realiza el {@code MaskedBeanSerializerModifier}
 * de Jackson. Este advice actúa como punto de extensión y verificación.
 * </p>
 *
 * @author Nova Platform
 */
@ControllerAdvice
public class MaskResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    /**
     * Crea una nueva instancia del advice de enmascaramiento.
     */
    public MaskResponseBodyAdvice() {
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                   MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request,
                                   ServerHttpResponse response) {
        // El enmascaramiento lo hace MaskedBeanSerializerModifier durante la serialización Jackson.
        // Retornamos el body sin modificar.
        return body;
    }
}
