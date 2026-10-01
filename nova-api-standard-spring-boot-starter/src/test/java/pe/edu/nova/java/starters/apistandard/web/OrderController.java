package pe.edu.nova.java.starters.apistandard.web;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.api.standard.error.DomainError;
import pe.edu.nova.java.libs.api.standard.error.FieldError;
import pe.edu.nova.java.libs.api.standard.error.InfrastructureError;

/** Un controlador que lanza un error de Nova de cada capa, como lo haría un caso de uso. */
@RestController
@RequestMapping("/orders")
class OrderController {

    /** El proveedor que falla en las pruebas: nunca tiene que llegar al cuerpo. */
    static final String UPSTREAM = "courier-acme";

    @GetMapping("/{id}")
    public Object find(@PathVariable("id") long id) {
        throw DomainError.notFound("ORDER_NOT_FOUND", "El pedido " + id + " no existe");
    }

    @PostMapping("/{id}/confirmations")
    public Object confirm(@PathVariable("id") long id) {
        throw DomainError.conflict("El pedido está cancelado");
    }

    @PostMapping
    public Object place() {
        throw ApplicationError.invalidInput("La solicitud tiene campos inválidos", List.of(
                FieldError.of("email", "El correo no es válido"),
                FieldError.of("quantity", "La cantidad debe ser positiva")));
    }

    @PostMapping("/{id}/payments")
    public Object pay(@PathVariable("id") long id) {
        throw ApplicationError.conflict("La operación sigue en curso", Duration.ofSeconds(1));
    }

    @GetMapping("/export")
    public Object export() {
        throw ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(30));
    }

    @GetMapping("/{id}/shipping")
    public Object shipping(@PathVariable("id") long id) {
        throw InfrastructureError.timeout(UPSTREAM, new SocketTimeoutException(UPSTREAM + ".interno:8443"));
    }

    @GetMapping("/{id}/stock")
    public Object stock(@PathVariable("id") long id) {
        throw InfrastructureError.unavailable("inventario", null);
    }
}
