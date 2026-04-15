plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":stockwellness-core"))
    testImplementation(testFixtures(project(":stockwellness-core")))

    // batch
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.batch:spring-batch-integration")

    // web (BatchAdminController가 있으면 유지)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // kafka
    implementation("org.springframework.kafka:spring-kafka")

    // resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")

    // flyway / db
//    implementation("org.flywaydb:flyway-core")
//    implementation("org.flywaydb:flyway-database-postgresql")
//    runtimeOnly("org.postgresql:postgresql")

    // logging
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.11.0")

    // tests
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testRuntimeOnly("com.h2database:h2")
}