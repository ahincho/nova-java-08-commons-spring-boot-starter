plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
}

val springBootVersion = "4.0.5"

dependencies {
    // api-standard library (from Maven Local)
    api("pe.edu.nova.java.libs:nova-api-standard:1.0.0")

    // Spring Boot auto-configuration
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-starter-jackson:$springBootVersion")

    // Configuration processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("org.junit.platform:junit-platform-launcher:6.0.3")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-08-commons-spring-boot-starter")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    val gpgKeyId: String? = System.getenv("GPG_SIGNING_KEY_ID")
    val gpgKey: String? = System.getenv("GPG_SIGNING_KEY")
    val gpgPassword: String? = System.getenv("GPG_SIGNING_PASSWORD")

    if (gpgKeyId != null && gpgKey != null) {
        useInMemoryPgpKeys(gpgKeyId, gpgKey, gpgPassword ?: "")
        sign(publishing.publications)
    }
}