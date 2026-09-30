package pe.edu.nova.java.starters.apistandard.web;

/**
 * El código de error que lleva cada status HTTP en el sobre: el catálogo de códigos de la
 * plataforma (ADR-031), el mismo en los tres stacks, para que un cliente que decide por el código se
 * comporte igual sin importar en qué stack está el servicio. Del ADR entra solo el catálogo; el
 * modelo de errores por capas llega después.
 * <p>
 * Un 4xx que el catálogo no nombra lleva {@link #REQUEST_ERROR}, y un 5xx que no nombra lleva
 * {@link #INTERNAL_SERVER_ERROR}.
 * </p>
 *
 * @author Nova Platform
 */
final class ErrorCodes {

    /** El código de un 4xx que el catálogo no nombra. */
    static final String REQUEST_ERROR = "REQUEST_ERROR";

    /** El código de un 500, y de todo 5xx que el catálogo no nombra. */
    static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    private ErrorCodes() {
    }

    /**
     * El código de error de un status.
     *
     * @param status el status HTTP del error, de 400 en adelante
     * @return el código del catálogo para ese status
     */
    static String of(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 408 -> "REQUEST_TIMEOUT";
            case 409 -> "CONFLICT";
            case 410 -> "GONE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 422 -> "UNPROCESSABLE_ENTITY";
            case 429 -> "TOO_MANY_REQUESTS";
            case 502 -> "BAD_GATEWAY";
            case 503 -> "SERVICE_UNAVAILABLE";
            case 504 -> "GATEWAY_TIMEOUT";
            default -> status >= 500 ? INTERNAL_SERVER_ERROR : REQUEST_ERROR;
        };
    }
}
