# Nova Commons Spring Boot Starter

A Gradle multi-project that re-exports the framework-free Nova libraries
as Spring Boot starters. The libraries themselves know nothing about
Spring; these modules are the wiring.

## Modules

| Module | Auto-configures | Wraps |
|---|---|---|
| `nova-api-standard-spring-boot-starter` | `ApiResponseInterceptor`, `GlobalExceptionHandler` | [nova-api-standard](https://github.com/ahincho/nova-java-01-api-standard) |
| `nova-mask-spring-boot-starter` | `MaskAutoConfiguration`, plus an Actuator health indicator and info contributor | [nova-mask-utils](https://github.com/ahincho/nova-java-04-mask-utils) |

Both register through `AutoConfiguration.imports`, so adding the
dependency is all the wiring an application does.

## Install

Published to GitHub Packages, so the repository needs to be declared and
authenticated with a token that has `read:packages`.

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-08-commons-spring-boot-starter")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("pe.edu.nova.java.starters:nova-api-standard-spring-boot-starter:3.0.0")
    implementation("pe.edu.nova.java.starters:nova-mask-spring-boot-starter:3.0.0")
}
```

Most applications should not depend on these directly — take
[nova-java-spring-boot-starter](https://github.com/ahincho/nova-java-12-spring-boot-starter),
the meta-starter that bundles them.

## What you get

**API standard.** Controllers returning a bare object are wrapped into
`ApiResponse<T>` by the interceptor, and every uncaught exception is answered
by `GlobalExceptionHandler` with the layered error model of
[ADR-031](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-031-modulo-de-errores-por-capas-con-trazabilidad.md),
so error shape stops depending on which developer wrote the endpoint.

A use case throws what went wrong, not an HTTP status:

```java
throw DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe");   // 404
throw ApplicationError.invalidInput("Campos inválidos", fieldErrors);       // 400
throw InfrastructureError.timeout("payments", exception);                  // 504
```

- **One log line per error**, written before any port runs, with `traceId`,
  `layer`, `code` and, for an upstream failure, `upstream`. `domain` and
  `application` go to `warn` without a stack trace; `infrastructure` and
  `platform` go to `error` with the cause.
- **The body** is the Nova envelope with `metadata.traceId`, the code of the
  platform catalog and its Spanish message. A 5xx never names the upstream.
  A request without a `traceId` gets a generated one, the same in the log and
  in the body.
- **`Retry-After`** for an application conflict, a rate limit and an
  unavailable dependency.
- **The `nova.errors` counter**, tagged with `layer` and `code`, when the
  service has Micrometer.
- **Spring MVC exceptions** keep their status and their headers: a 4xx is
  `application`, a 502, 503 or 504 is `infrastructure`, any other 5xx is
  `platform`. Anything else is a `PlatformError`, answered as a 500.

The three ports of ADR-031 are beans with `@ConditionalOnMissingBean`:
`ErrorStatusMapper`, `ErrorCatalog` and `ErrorSerializer`. A service, or the
starter of an organization such as UTP, declares its own and this starter
uses it, without forking. The ports only ever receive a `SanitizedFailure`,
so a port written by an organization cannot leak the upstream or the cause.

## Migrating to 3.0.0

The 3.0.0 starters answer errors with ADR-031, and that changes what a client
sees:

| Before (2.x) | From 3.0.0 | What to do |
|---|---|---|
| code `ERROR` from `ApiResponse.error(...)` in an exception | the catalog code of the status, or the error's own code | compare against the catalog code instead of `ERROR` |
| validation errors with code `VALIDATION_ERROR` | `BAD_REQUEST`, with the same field errors | compare against `BAD_REQUEST` |
| `IllegalArgumentException` answered as 400 with its message | a `PlatformError`, answered as 500 with the generic message | throw `ApplicationError.invalidInput(...)` for invalid input |
| generic 4xx messages such as `Not Found` | the Spanish catalog messages, such as `El recurso no existe` | do not compare against the message text |
| a 5xx said `Error interno del servidor` whatever its status | each status has its own message, such as `El servicio no está disponible en este momento` for a 503 | read the code, not the message |
| `metadata` was `null` in an error | `metadata.traceId` and `metadata.timestamp` | nothing; quote the `traceId` when reporting a failure |
| `ErrorCodes` and `GlobalExceptionHandler.envelope(...)` | removed | use `NovaErrorCatalog.platformCode(status)` or the `ErrorPorts` bean |
| `new GlobalExceptionHandler()` | `new GlobalExceptionHandler(ports, counter)` | let the auto-configuration create it |

The same version carries the envelope fixes that were waiting for 2.0.1: the body says the real
status, a 4xx or 5xx without a body says `success: false`, the 4xx of Spring
MVC are no longer 500, and the actuator and the error controller of Spring
Boot are left unwrapped.

**Masking.** `MaskEngine` becomes a bean, and Actuator gains a health
indicator plus an info contributor reporting which strategies are
registered.

## Requirements

Java 25, Spring Boot 4.

## License

Eclipse Public License 2.0 — see [LICENSE](LICENSE).

Copyright © 2026 Angel Hincho.
