package pe.edu.nova.java.starters.mask.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import pe.edu.nova.java.libs.mask.utils.CountryCode;

/**
 * Propiedades de configuración del starter mask-utils.
 * <p>
 * Se vinculan al prefijo {@code nova.mask} en
 * {@code application.properties} o {@code application.yml}.
 * </p>
 *
 * @author Nova Platform
 */
@ConfigurationProperties(prefix = "nova.mask")
public class MaskProperties {

    /** Habilita o deshabilita todo el starter. */
    private boolean enabled = true;

    /** Código de país ISO 3166-1 alpha-2 predeterminado. */
    private String defaultCountry = CountryCode.fromLocale().name();

    /** Carácter de máscara predeterminado. */
    private char defaultMaskChar = '*';

    /** Configuración de enmascaramiento de logs. */
    private Log log = new Log();

    /** Configuración de enmascaramiento de respuestas REST. */
    private Response response = new Response();

    /** Crea una nueva instancia con valores predeterminados. */
    public MaskProperties() {
    }

    /**
     * Indica si el starter está habilitado.
     *
     * @return {@code true} si el starter está habilitado
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Establece si el starter está habilitado.
     *
     * @param enabled {@code true} para habilitar, {@code false} para deshabilitar
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Retorna el código de país predeterminado.
     *
     * @return código de país ISO 3166-1 alpha-2
     */
    public String getDefaultCountry() {
        return defaultCountry;
    }

    /**
     * Establece el código de país predeterminado.
     *
     * @param defaultCountry código de país ISO 3166-1 alpha-2
     */
    public void setDefaultCountry(String defaultCountry) {
        this.defaultCountry = defaultCountry;
    }

    /**
     * Retorna el carácter de máscara predeterminado.
     *
     * @return carácter de máscara
     */
    public char getDefaultMaskChar() {
        return defaultMaskChar;
    }

    /**
     * Establece el carácter de máscara predeterminado.
     *
     * @param defaultMaskChar carácter de máscara
     */
    public void setDefaultMaskChar(char defaultMaskChar) {
        this.defaultMaskChar = defaultMaskChar;
    }

    /**
     * Retorna la configuración de enmascaramiento de logs.
     *
     * @return configuración de logs
     */
    public Log getLog() {
        return log;
    }

    /**
     * Establece la configuración de enmascaramiento de logs.
     *
     * @param log configuración de logs
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /**
     * Retorna la configuración de enmascaramiento de respuestas REST.
     *
     * @return configuración de respuestas
     */
    public Response getResponse() {
        return response;
    }

    /**
     * Establece la configuración de enmascaramiento de respuestas REST.
     *
     * @param response configuración de respuestas
     */
    public void setResponse(Response response) {
        this.response = response;
    }

    /**
     * Configuración de enmascaramiento de logs.
     */
    public static class Log {

        /** Habilita el enmascaramiento de logs. */
        private boolean enabled = true;

        /** Habilita la auto-detección de patrones sensibles en logs. */
        private boolean autoDetect = true;

        /** Crea una nueva instancia con valores predeterminados. */
        public Log() {
        }

        /**
         * Indica si el enmascaramiento de logs está habilitado.
         *
         * @return {@code true} si está habilitado
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Establece si el enmascaramiento de logs está habilitado.
         *
         * @param enabled {@code true} para habilitar
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Indica si la auto-detección de patrones sensibles está habilitada.
         *
         * @return {@code true} si está habilitada
         */
        public boolean isAutoDetect() {
            return autoDetect;
        }

        /**
         * Establece si la auto-detección de patrones sensibles está habilitada.
         *
         * @param autoDetect {@code true} para habilitar
         */
        public void setAutoDetect(boolean autoDetect) {
            this.autoDetect = autoDetect;
        }
    }

    /**
     * Configuración de enmascaramiento de respuestas REST.
     */
    public static class Response {

        /** Habilita el enmascaramiento de respuestas REST. */
        private boolean enabled = true;

        /** Crea una nueva instancia con valores predeterminados. */
        public Response() {
        }

        /**
         * Indica si el enmascaramiento de respuestas REST está habilitado.
         *
         * @return {@code true} si está habilitado
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Establece si el enmascaramiento de respuestas REST está habilitado.
         *
         * @param enabled {@code true} para habilitar
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
