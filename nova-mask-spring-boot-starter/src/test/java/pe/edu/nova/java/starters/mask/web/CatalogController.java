package pe.edu.nova.java.starters.mask.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.nova.java.libs.mask.utils.MaskType;
import pe.edu.nova.java.libs.mask.utils.annotation.Masked;
import pe.edu.nova.java.libs.mask.utils.annotation.MaskedClass;
import pe.edu.nova.java.libs.mask.utils.annotation.SkipMasking;

/**
 * El controlador del servicio de las pruebas, con una respuesta por cada forma en que un servicio
 * real puede declarar sus campos: sin anotación, con {@code @Masked}, con {@code @MaskedClass} y con
 * {@code @SkipMasking}.
 */
@RestController
@RequestMapping("/catalog")
class CatalogController {

    /**
     * Un producto: su {@code name} es un nombre comercial, no un dato personal.
     *
     * @return el producto
     */
    @GetMapping("/products/1")
    public Product product() {
        return new Product(1, "Taza", "Hogar");
    }

    /**
     * Un proveedor, con un objeto de transferencia sin ninguna anotación.
     *
     * @return el proveedor
     */
    @GetMapping("/suppliers/1")
    public Supplier supplier() {
        return new Supplier(1, "Ceramicas del Sur", "ventas@ceramicasdelsur.pe", "+51 987 654 321");
    }

    /**
     * Un cliente, con los datos personales anotados con {@code @Masked}.
     *
     * @return el cliente
     */
    @GetMapping("/customers/1")
    public Customer customer() {
        return new Customer(1, "Juan Perez", "juan.perez@acme.pe", "+51 987 654 321");
    }

    /**
     * Un paciente, de una clase que pide enmascarar sus campos con {@code @MaskedClass}.
     *
     * @return el paciente
     */
    @GetMapping("/patients/1")
    public Patient patient() {
        return new Patient(1, "Juan Perez", "juan.perez@acme.pe", "Gripe estacional");
    }

    /**
     * Un empleado, de una clase que pide enmascarar salvo un campo con {@code @SkipMasking}.
     *
     * @return el empleado
     */
    @GetMapping("/employees/1")
    public Employee employee() {
        return new Employee(1, "Juan Perez", "juan.perez@acme.pe");
    }

    /**
     * Un producto.
     *
     * @param id el identificador
     * @param name el nombre comercial
     * @param category la categoría
     */
    public record Product(long id, String name, String category) {}

    /**
     * Un proveedor sin anotaciones.
     *
     * @param id el identificador
     * @param name la razón social
     * @param email el correo comercial
     * @param phone el teléfono comercial
     */
    public record Supplier(long id, String name, String email, String phone) {}

    /**
     * Un cliente con los datos personales marcados.
     *
     * @param id el identificador
     * @param name el nombre, sin anotación
     * @param email el correo, marcado
     * @param phone el teléfono, marcado
     */
    public record Customer(
            long id,
            String name,
            @Masked(type = MaskType.EMAIL) String email,
            @Masked(type = MaskType.PHONE) String phone) {}

    /**
     * Un paciente, cuya clase pide enmascarar.
     *
     * @param id el identificador
     * @param name el nombre
     * @param email el correo
     * @param diagnosis el diagnóstico, que no tiene un nombre conocido
     */
    @MaskedClass
    public record Patient(long id, String name, String email, String diagnosis) {}

    /**
     * Un empleado, cuya clase pide enmascarar salvo su correo de trabajo.
     *
     * @param id el identificador
     * @param name el nombre
     * @param email el correo de trabajo, público
     */
    @MaskedClass
    public record Employee(long id, String name, @SkipMasking String email) {}
}
