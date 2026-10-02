package pe.edu.nova.java.starters.mask.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pe.edu.nova.java.libs.mask.utils.CountryCode;
import pe.edu.nova.java.libs.mask.utils.MaskEngine;
import pe.edu.nova.java.libs.mask.utils.MaskType;
import pe.edu.nova.java.libs.mask.utils.annotation.MaskConfigAnnotation;
import pe.edu.nova.java.libs.mask.utils.annotation.Masked;
import pe.edu.nova.java.libs.mask.utils.annotation.MaskedClass;
import pe.edu.nova.java.libs.mask.utils.annotation.SkipMasking;
import pe.edu.nova.java.starters.mask.autoconfigure.MaskAutoConfiguration;
import pe.edu.nova.java.starters.mask.autoconfigure.MaskJacksonAutoConfiguration;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Qué enmascara el serializador de Jackson y cuándo. Lo que lleva {@code @Masked} o
 * {@code @MaskedClass} se enmascara siempre; inferir por el nombre del campo es opt-in, con
 * {@code nova.mask.infer-by-field-name}, porque el serializador es global y enmascarar un
 * {@code name} cualquiera cambia las respuestas del servicio.
 *
 * <p>Cada prueba serializa con el {@code JsonMapper} que arma Spring Boot, el mismo que usa un
 * servicio real, y no con un mapper armado a mano.</p>
 */
class MaskedBeanSerializerModifierTest {

    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

    private static final String INFER = "nova.mask.infer-by-field-name=true";

    /**
     * El contexto de un servicio con el starter. El país se fija para que el resultado no dependa del
     * locale de la máquina que corre las pruebas.
     */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class, MaskAutoConfiguration.class, MaskJacksonAutoConfiguration.class))
            .withPropertyValues("nova.mask.default-country=PE");

    // --- Por defecto: solo lo anotado ---------------------------------------------------------------

    @Test
    void aBeanWithoutAnnotationsIsNotMaskedByDefault() {
        Map<String, Object> json = serialize(contact());

        assertThat(json)
                .containsEntry("name", "Juan Perez")
                .containsEntry("email", "juan.perez@acme.pe")
                .containsEntry("phone", "+51 987 654 321")
                .containsEntry("dni", "12345678")
                .containsEntry("card", "4111111111111111")
                .containsEntry("account", "00219300123456789012")
                .containsEntry("ip", "192.168.10.25");
    }

    @Test
    void aProductNameIsAnsweredInTheClearByDefault() {
        Map<String, Object> json = serialize(new Product(7, "Camiseta azul", 3));

        assertThat(json).containsEntry("name", "Camiseta azul");
    }

    @Test
    void onlyTheFieldsThatCarryMaskedAreMaskedByDefault() {
        Map<String, Object> json = serialize(subscriber());

        assertThat(json)
                .containsEntry("email", "j*********@acme.pe")
                .containsEntry("phone", "+51 *** *** 321")
                .containsEntry("name", "Juan Perez")
                .containsEntry("ip", "192.168.10.25");
    }

    @Test
    void aMaskedFieldWithoutATypeTakesItFromItsNameWithoutTheProperty() {
        Map<String, Object> json = serialize(new Caller("+51 987 654 321"));

        assertThat(json).containsEntry("phone", masked(MaskType.PHONE, "+51 987 654 321"));
    }

    @Test
    void maskedWorksOnTheFieldOfAClassWithGetters() {
        Map<String, Object> json = serialize(new Account("juan.perez@acme.pe", "Juan Perez"));

        assertThat(json).containsEntry("email", "j*********@acme.pe").containsEntry("name", "Juan Perez");
    }

    @Test
    void aMaskedClassMasksItsKnownFieldsWithoutTheProperty() {
        Map<String, Object> json = serialize(patient());

        assertThat(json)
                .containsEntry("name", "J*** P****")
                .containsEntry("email", "j*********@acme.pe")
                .containsEntry("dni", masked(MaskType.IDENTITY_DOCUMENT, "12345678"));
    }

    @Test
    void aMaskedClassLeavesTheFieldsWithoutAKnownNameAndTheNonStringsAlone() {
        Map<String, Object> json = serialize(patient());

        assertThat(json).containsEntry("diagnosis", "Gripe estacional").containsEntry("age", 41);
    }

    @Test
    void theMaskedClassAnnotationOnlyReachesItsOwnClass() {
        Map<String, Object> json = serialize(new Visit(contact(), patient()));

        assertThat(asMap(json.get("visitor"))).containsEntry("name", "Juan Perez");
        assertThat(asMap(json.get("patient"))).containsEntry("name", "J*** P****");
    }

    @Test
    void aMaskedClassUsesTheMaskCharOfItsConfiguration() {
        Map<String, Object> json = serialize(new Beneficiary("Juan Perez"));

        assertThat(json).containsEntry("name", "J### P####");
    }

    @Test
    void aMaskedClassStillRespectsSkipMaskingOnAField() {
        Map<String, Object> json = serialize(new Doctor("Juan Perez", "juan.perez@acme.pe"));

        assertThat(json).containsEntry("name", "J*** P****").containsEntry("email", "juan.perez@acme.pe");
    }

    @Test
    void skipMaskingOnAClassKeepsItInTheClearButNotWhatCarriesMasked() {
        Map<String, Object> json = serialize(publicDirectory());

        assertThat(json)
                .containsEntry("name", "Juan Perez")
                .containsEntry("email", "juan.perez@acme.pe")
                .containsEntry("phone", "+51 *** *** 321");
    }

    // --- Con la propiedad: se infiere por el nombre ------------------------------------------------

    @Test
    void withInferenceOnTheKnownNamesAreMasked() {
        Map<String, Object> json = serialize(contact(), INFER);

        assertThat(json)
                .containsEntry("name", "J*** P****")
                .containsEntry("email", "j*********@acme.pe")
                .containsEntry("phone", "+51 *** *** 321")
                .containsEntry("dni", masked(MaskType.IDENTITY_DOCUMENT, "12345678"))
                .containsEntry("card", masked(MaskType.CREDIT_CARD, "4111111111111111"))
                .containsEntry("account", masked(MaskType.BANK_ACCOUNT, "00219300123456789012"))
                .containsEntry("ip", masked(MaskType.IP_ADDRESS, "192.168.10.25"));
    }

    @Test
    void withInferenceOnTheFieldsWithoutAKnownNameAndTheNonStringsAreLeftAlone() {
        Map<String, Object> json = serialize(contact(), INFER);

        assertThat(json).containsEntry("description", "Cliente frecuente").containsEntry("units", 3);
    }

    @Test
    void withInferenceOnTheNamesAreMatchedWithoutLookingAtTheCase() {
        Map<String, Object> json = serialize(new Person("Juan", "Perez"), INFER);

        assertThat(json).containsEntry("firstName", "J***").containsEntry("lastName", "P****");
    }

    @Test
    void withInferenceOnAFieldThatIsNotAStringIsNeverTouched() {
        Map<String, Object> json = serialize(new Counters(12, 4_111_111_111_111_111L), INFER);

        assertThat(json).containsEntry("name", 12).containsEntry("card", 4_111_111_111_111_111L);
    }

    @Test
    void withInferenceOnSkipMaskingStillKeepsAFieldInTheClear() {
        Map<String, Object> json = serialize(new Doctor("Juan Perez", "juan.perez@acme.pe"), INFER);

        assertThat(json).containsEntry("name", "J*** P****").containsEntry("email", "juan.perez@acme.pe");
    }

    @Test
    void withInferenceOnSkipMaskingOnAClassStillKeepsItsFieldsInTheClear() {
        Map<String, Object> json = serialize(publicDirectory(), INFER);

        assertThat(json)
                .containsEntry("name", "Juan Perez")
                .containsEntry("email", "juan.perez@acme.pe")
                .containsEntry("phone", "+51 *** *** 321");
    }

    @Test
    void withInferenceOnAMaskedFieldKeepsItsExplicitType() {
        Map<String, Object> json = serialize(subscriber(), INFER);

        assertThat(json).containsEntry("email", "j*********@acme.pe").containsEntry("phone", "+51 *** *** 321");
    }

    @Test
    void withInferenceOnTheElementsOfACollectionAreMaskedToo() {
        Map<String, Object> json = serialize(new Agenda(List.of(contact())), INFER);

        List<?> contacts = (List<?>) json.get("contacts");
        assertThat(asMap(contacts.get(0))).containsEntry("name", "J*** P****");
    }

    // --- Apagado: nada se enmascara -----------------------------------------------------------------

    @Test
    void withTheStarterDisabledNothingIsMaskedNotEvenWhatCarriesMasked() {
        Map<String, Object> json = serialize(subscriber(), "nova.mask.enabled=false", INFER);

        assertThat(json)
                .containsEntry("email", "juan.perez@acme.pe")
                .containsEntry("phone", "+51 987 654 321")
                .containsEntry("name", "Juan Perez");
    }

    @Test
    void withTheStarterDisabledAMaskedClassIsNotMaskedEither() {
        Map<String, Object> json = serialize(patient(), "nova.mask.enabled=false");

        assertThat(json).containsEntry("name", "Juan Perez").containsEntry("email", "juan.perez@acme.pe");
    }

    // --- Utilidades ---------------------------------------------------------------------------------

    /** Serializa con el {@code JsonMapper} del servicio y devuelve el objeto JSON que contestaría. */
    private Map<String, Object> serialize(Object bean, String... properties) {
        AtomicReference<Map<String, Object>> json = new AtomicReference<>();
        runner.withPropertyValues(properties).run(context -> {
            JsonMapper mapper = context.getBean(JsonMapper.class);
            json.set(mapper.readValue(mapper.writeValueAsString(bean), JSON_OBJECT));
        });
        return json.get();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    /** Lo que la librería enmascara para ese tipo, sin repetir aquí las reglas de cada estrategia. */
    private static String masked(MaskType type, String value) {
        return MaskEngine.mask(value, type, CountryCode.PE).maskedValue();
    }

    private static Contact contact() {
        return new Contact(
                "Juan Perez",
                "juan.perez@acme.pe",
                "+51 987 654 321",
                "12345678",
                "4111111111111111",
                "00219300123456789012",
                "192.168.10.25",
                "Cliente frecuente",
                3);
    }

    private static Subscriber subscriber() {
        return new Subscriber("juan.perez@acme.pe", "+51 987 654 321", "Juan Perez", "192.168.10.25");
    }

    private static Patient patient() {
        return new Patient("Juan Perez", "juan.perez@acme.pe", "12345678", "Gripe estacional", 41);
    }

    private static PublicDirectory publicDirectory() {
        return new PublicDirectory("Juan Perez", "juan.perez@acme.pe", "+51 987 654 321");
    }

    // --- Los objetos que se serializan --------------------------------------------------------------

    /** Un objeto como el de cualquier servicio: ningún campo lleva anotación. */
    record Contact(
            String name,
            String email,
            String phone,
            String dni,
            String card,
            String account,
            String ip,
            String description,
            int units) {}

    /** Un producto, cuyo {@code name} no es un dato personal. */
    record Product(long id, String name, int stock) {}

    /** Solo algunos campos llevan {@code @Masked}, con su tipo. */
    record Subscriber(
            @Masked(type = MaskType.EMAIL) String email,
            @Masked(type = MaskType.PHONE) String phone,
            String name,
            String ip) {}

    /** {@code @Masked} sin tipo: lo toma del nombre del campo. */
    record Caller(@Masked String phone) {}

    /** Una clase con campos privados y getters, como la de un servicio que no usa records. */
    static class Account {

        @Masked(type = MaskType.EMAIL)
        private final String email;

        private final String name;

        Account(String email, String name) {
            this.email = email;
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }
    }

    /** Una clase que pide enmascarar por el nombre de sus campos. */
    @MaskedClass
    record Patient(String name, String email, String dni, String diagnosis, int age) {}

    /** Una clase que pide enmascarar con su propio carácter. */
    @MaskedClass
    @MaskConfigAnnotation(maskChar = '#')
    record Beneficiary(String name) {}

    /** Una clase que pide enmascarar, salvo un campo que se queda en claro. */
    @MaskedClass
    record Doctor(String name, @SkipMasking String email) {}

    /** Una clase que no se enmascara por nombre, salvo lo que lleva {@code @Masked}. */
    @SkipMasking
    record PublicDirectory(String name, String email, @Masked(type = MaskType.PHONE) String phone) {}

    /** Un objeto con otros dentro, uno de ellos con {@code @MaskedClass}. */
    record Visit(Contact visitor, Patient patient) {}

    /** Una lista de contactos. */
    record Agenda(List<Contact> contacts) {}

    /** Nombres en camelCase. */
    record Person(String firstName, String lastName) {}

    /** Campos que se llaman como uno sensible pero no son texto. */
    record Counters(int name, long card) {}
}
