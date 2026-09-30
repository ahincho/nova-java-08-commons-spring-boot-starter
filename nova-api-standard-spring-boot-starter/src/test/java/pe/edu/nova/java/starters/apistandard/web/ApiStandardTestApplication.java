package pe.edu.nova.java.starters.apistandard.web;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * El servicio de las pruebas: una aplicación Spring Boot con el starter, Bean Validation y
 * Actuator. No escanea componentes, así que el starter entra solo por su auto-configuración, igual
 * que en un servicio real.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({ItemController.class, ItemExceptionHandler.class})
class ApiStandardTestApplication {

    /**
     * Un indicador de salud caído, para que {@code /actuator/health} conteste 503.
     *
     * @return el indicador
     */
    @Bean
    HealthIndicator inventoryHealthIndicator() {
        return () -> Health.down().build();
    }
}
