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
    }
}
