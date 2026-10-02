package pe.edu.nova.java.starters.mask.web;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * El servicio de las pruebas: una aplicación Spring Boot con el starter y Actuator. No escanea
 * componentes, así que el starter entra solo por su auto-configuración, igual que en un servicio
 * real.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(CatalogController.class)
class MaskTestApplication {}
