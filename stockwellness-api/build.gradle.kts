plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":stockwellness-core"))
    testImplementation(testFixtures(project(":stockwellness-core")))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // kafka
    implementation("org.springframework.kafka:spring-kafka")

    // ai / external http
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")
    implementation("org.apache.httpcomponents.client5:httpclient5")

    // flyway / db
//    implementation("org.flywaydb:flyway-core")
//    implementation("org.flywaydb:flyway-database-postgresql")
//    runtimeOnly("org.postgresql:postgresql")

    // logging
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.11.0")

    // docs
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.2")
    implementation("org.webjars:swagger-ui:5.10.3")
    implementation("org.webjars:webjars-locator-core")

    // tests
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("com.h2database:h2")

    // docker
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
}