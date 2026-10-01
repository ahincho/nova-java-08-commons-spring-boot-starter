import org.gradle.api.plugins.quality.CheckstyleExtension
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension

plugins {
    id("net.nemerosa.versioning") version "4.0.1"
    id("org.owasp.dependencycheck") version "12.2.2" apply false
    id("org.cyclonedx.bom") version "3.2.4"
}

versioning {
    releaseMode = "snapshot"
    displayMode = "snapshot"
    releaseBuild = false
}

subprojects {
    group = "pe.edu.nova.java.starters"
    version = findProperty("version") as String
    
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")
    apply(plugin = "org.owasp.dependencycheck")

    // Versiones parcheadas de dependencias que el OWASP gate marca con CVSS >= 7.
    // Spring Boot llega solo como compileOnly y la resolución no se publica, así que
    // esto cambia lo que compila y analiza el starter, no lo que recibe el consumidor.
    // Verificadas contra la GitHub Advisory Database el 2026-09-27.
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.apache.tomcat.embed") {
                useVersion("11.0.26")
                because("CVE-2026-68525, CVE-2026-65905, CVE-2026-65182 require Tomcat 11.0.25+; Spring Boot 4.0.8 pins 11.0.24")
            }
            // Las tres siguientes llegan por las dependencias de la herramienta checkstyle.
            if (requested.group == "org.apache.httpcomponents.core5" && requested.name.startsWith("httpcore5")) {
                useVersion("5.4.3")
                because("CVE-2026-54399 requires httpcore5 5.4.3+")
            }
            if (requested.group == "commons-beanutils" && requested.name == "commons-beanutils") {
                useVersion("1.11.0")
                because("CVE-2025-48734 requires commons-beanutils 1.11.0+")
            }
            if (requested.group == "org.codehaus.plexus" && requested.name == "plexus-utils") {
                useVersion("3.6.1")
                because("CVE-2025-67030 requires plexus-utils 3.6.1+")
            }
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        // Internal Nova Platform dependencies (each lives in its own repo/package).
        // GITHUB_TOKEN cannot read packages from another repo, so this needs a PAT
        // (falls back to GITHUB_TOKEN for local/manual builds where only that is set).
        val readToken = System.getenv("NOVA_PACKAGES_READ_TOKEN") ?: System.getenv("GITHUB_TOKEN")
        maven {
            name = "NovaMaskUtils"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-04-mask-utils")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = readToken
            }
        }
        maven {
            name = "NovaApiStandard"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-01-api-standard")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = readToken
            }
        }
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }

    tasks.named<Javadoc>("javadoc") {
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:all", "-quiet")
            encoding = "UTF-8"
            charSet = "UTF-8"
        }
    }

    configure<CheckstyleExtension> {
        // Only lint production code. Test suites commonly rely on static-import
        // wildcards (org.junit.jupiter.api.Assertions.*, net.jqwik.api.*), which
        // is an accepted convention that would otherwise trip AvoidStarImport.
        sourceSets = listOf(the<SourceSetContainer>().getByName("main"))
    }

    configure<DependencyCheckExtension> {
        // NVD_API_KEY / NOVA_OWASP_FAIL_ON_CVSS are injected by reusable-owasp-check.yml.
        // Locally (no env vars set) this defaults to "never fail" (11.0, matches plugin default)
        // and an empty NVD key (slower updates, acceptable for local dev).
        failBuildOnCVSS = (System.getenv("NOVA_OWASP_FAIL_ON_CVSS") ?: "11").toFloat()
        nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
        // La ruta donde reusable-owasp-check.yml restaura el mirror NVD compartido. Sin ella el plugin
        // usa su ruta por defecto, no ve el mirror y descarga la base entera, que no termina en 45 minutos.
        data.directory = System.getenv("NOVA_OWASP_DATA_DIR")
            ?: "${System.getProperty("user.home")}/.dependency-check-data"
    }
}
