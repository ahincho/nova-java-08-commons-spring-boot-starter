package pe.edu.nova.java.starters.apistandard.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * El controlador del servicio de las pruebas, con un caso por cada forma en que contesta un
 * controlador real: con su propio status, sin cuerpo, con una excepción de Spring o con un cuerpo
 * que no es JSON.
 */
@RestController
@RequestMapping("/items")
class ItemController {

    /** El header que exige la creación de un ítem. */
    static final String TENANT_HEADER = "X-Tenant";

    /** El único ítem que existe. */
    static final Item TABLE = new Item(1, "Mesa", 4);

    /** El reporte binario de un ítem. */
    static final byte[] REPORT = {0x4E, 0x6F, 0x76, 0x61, 0x00, (byte) 0xFF};

    /** El manual de un ítem, servido como {@link Resource}. */
    static final byte[] MANUAL = "%PDF-1.7 manual de la mesa".getBytes(StandardCharsets.UTF_8);

    /**
     * Lista los ítems: contesta 200 con un cuerpo.
     *
     * @return los ítems
     */
    @GetMapping
    public List<Item> list() {
        return List.of(TABLE);
    }

    /**
     * Crea un ítem: contesta 201 con {@code ResponseEntity}, y exige un header y un cuerpo válido.
     *
     * @param tenant el inquilino
     * @param item el ítem nuevo
     * @return el ítem creado
     */
    @PostMapping
    public ResponseEntity<Item> create(@RequestHeader(TENANT_HEADER) String tenant, @Valid @RequestBody NewItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new Item(2, item.name(), item.quantity()));
    }

    /**
     * Copia un ítem: contesta 201 con {@code @ResponseStatus}.
     *
     * @param id el ítem
     * @return la copia
     */
    @PostMapping("/{id}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public Item copy(@PathVariable("id") long id) {
        return new Item(3, TABLE.name(), TABLE.quantity());
    }

    /**
     * Busca un ítem como lo hace un servicio real, con {@code ResponseEntity.of}: 404 sin cuerpo si
     * no existe.
     *
     * @param id el ítem
     * @return el ítem, o 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> find(@PathVariable("id") long id) {
        return ResponseEntity.of(Optional.of(TABLE).filter(item -> item.id() == id));
    }

    /**
     * Borra un ítem: contesta 204 sin cuerpo.
     *
     * @param id el ítem
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) {
        return ResponseEntity.noContent().build();
    }

    /**
     * Confirma un ítem: contesta 200 sin cuerpo, con {@code ResponseEntity.ok().build()}.
     *
     * @param id el ítem
     * @return 200 sin cuerpo
     */
    @PostMapping("/{id}/confirmations")
    public ResponseEntity<Void> confirm(@PathVariable("id") long id) {
        return ResponseEntity.ok().build();
    }

    /**
     * Reemplaza un ítem: contesta 409 con un cuerpo propio del controlador.
     *
     * @param id el ítem
     * @return 409 con el motivo
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> replace(@PathVariable("id") long id) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("reason", "El ítem está en un pedido abierto"));
    }

    /**
     * Consulta el stock de un ítem: contesta 503 sin cuerpo.
     *
     * @param id el ítem
     * @return 503
     */
    @GetMapping("/{id}/stock")
    public ResponseEntity<Item> stock(@PathVariable("id") long id) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    /**
     * Busca ítems por nombre: exige un parámetro.
     *
     * @param name el nombre
     * @return los ítems con ese nombre
     */
    @GetMapping("/search")
    public List<Item> search(@RequestParam("name") String name) {
        return List.of(TABLE).stream().filter(item -> item.name().equals(name)).toList();
    }

    /**
     * Lista los primeros ítems: el parámetro lleva una restricción de Bean Validation.
     *
     * @param limit cuántos
     * @return los ítems
     */
    @GetMapping("/top")
    public List<Item> top(@RequestParam("limit") @Max(value = 10, message = "No se pueden pedir más de 10") int limit) {
        return List.of(TABLE);
    }

    /**
     * Reserva un ítem: falla con un 409 de {@link ResponseStatusException}.
     *
     * @param id el ítem
     * @return nunca retorna
     */
    @PostMapping("/{id}/reservations")
    public Item reserve(@PathVariable("id") long id) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "El ítem ya está reservado");
    }

    /**
     * Consulta el precio de un ítem: falla con un 503 que nombra al proveedor caído.
     *
     * @param id el ítem
     * @return nunca retorna
     */
    @GetMapping("/{id}/price")
    public Item price(@PathVariable("id") long id) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de precios de Acme no responde");
    }

    /**
     * Valida un ítem: falla con {@link IllegalArgumentException}.
     *
     * @param id el ítem
     * @return nunca retorna
     */
    @GetMapping("/{id}/check")
    public Item check(@PathVariable("id") long id) {
        throw new IllegalArgumentException("El ítem no es válido");
    }

    /**
     * Audita un ítem: falla con una excepción que nadie maneja.
     *
     * @param id el ítem
     * @return nunca retorna
     */
    @GetMapping("/{id}/audit")
    public Item audit(@PathVariable("id") long id) {
        throw new IllegalStateException("La auditoría de Acme falló");
    }

    /**
     * Bloquea un ítem: falla con una excepción que maneja el propio servicio.
     *
     * @param id el ítem
     * @return nunca retorna
     */
    @PostMapping("/{id}/locks")
    public Item lock(@PathVariable("id") long id) {
        throw new ItemExceptionHandler.ItemLockedException("inventario");
    }

    /**
     * Descarga el reporte de un ítem como {@code byte[]}.
     *
     * @param id el ítem
     * @return el reporte
     */
    @GetMapping(value = "/{id}/report", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] report(@PathVariable("id") long id) {
        return REPORT;
    }

    /**
     * Descarga el manual de un ítem como {@link Resource}.
     *
     * @param id el ítem
     * @return el manual
     */
    @GetMapping(value = "/{id}/manual", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> manual(@PathVariable("id") long id) {
        return ResponseEntity.ok(new ByteArrayResource(MANUAL));
    }

    /**
     * Devuelve el nombre de un ítem como texto.
     *
     * @param id el ítem
     * @return el nombre
     */
    @GetMapping(value = "/{id}/name", produces = MediaType.TEXT_PLAIN_VALUE)
    public String name(@PathVariable("id") long id) {
        return TABLE.name();
    }

    /**
     * Un ítem.
     *
     * @param id el identificador
     * @param name el nombre
     * @param quantity las unidades
     */
    public record Item(long id, String name, int quantity) {}

    /**
     * Un ítem por crear.
     *
     * @param name el nombre
     * @param quantity las unidades
     */
    public record NewItem(
            @NotBlank(message = "El nombre es obligatorio") String name,
            @Positive(message = "La cantidad debe ser positiva") int quantity) {}
}
