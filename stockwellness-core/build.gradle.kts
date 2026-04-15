plugins {
    `java-library`
    id("io.spring.dependency-management")
    `java-test-fixtures`
}

val generatedDir = layout.buildDirectory.dir("generated/querydsl").get().asFile

dependencies {
    // domain calculation
    api("org.ta4j:ta4j-core:0.15")

    // jpa
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // validation
    api("org.springframework.boot:spring-boot-starter-validation")

    // jackson
    api("com.fasterxml.jackson.datatype:jackson-datatype-hibernate6")

    // redis
    api("org.springframework.boot:spring-boot-starter-data-redis")

    //db
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // querydsl
    api("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // spring-ai / external http
    api("org.springframework.ai:spring-ai-openai-spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-web") // for RestClient
    api("org.springframework.retry:spring-retry")

    // resilience4j
    api("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    api("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")

    // kafka
    api("org.springframework.kafka:spring-kafka")

    // logging (p6spy)
    api("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.11.0")

    // utils
    api("org.apache.commons:commons-lang3")
    api("io.projectreactor:reactor-core")
    api("org.apache.httpcomponents.client5:httpclient5")

    // tests
    testRuntimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesImplementation("org.springframework.boot:spring-boot-testcontainers")
    testFixturesImplementation("org.testcontainers:postgresql")
    testFixturesImplementation("org.testcontainers:kafka")
}