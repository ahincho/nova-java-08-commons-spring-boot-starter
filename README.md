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
    implementation("pe.edu.nova.java.starters:nova-api-standard-spring-boot-starter:2.0.0")
    implementation("pe.edu.nova.java.starters:nova-mask-spring-boot-starter:2.0.0")
}
```

Most applications should not depend on these directly — take
[nova-java-spring-boot-starter](https://github.com/ahincho/nova-java-12-spring-boot-starter),
the meta-starter that bundles them.

## What you get

**API standard.** Controllers returning a bare object are wrapped into
`ApiResponse<T>` by the interceptor, and every uncaught exception becomes
an `ApiError` with the right status through `GlobalExceptionHandler` —
so error shape stops depending on which developer wrote the endpoint.

**Masking.** `MaskEngine` becomes a bean, and Actuator gains a health
indicator plus an info contributor reporting which strategies are
registered.

## Requirements

Java 25, Spring Boot 4.

## License

Eclipse Public License 2.0 — see [LICENSE](LICENSE).

Copyright © 2026 Angel Hincho.
