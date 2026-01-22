plugins {
    java
    // [변경 1] Spring Boot 3.4.1 (최신 안정 버전)
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.epages.restdocs-api-spec") version "0.19.2"
}

group = "org"
version = "0.0.1-SNAPSHOT"
description = "stockwellness"

val springAiVersion = "1.0.0-M1"
// [변경 2] Spring Cloud 버전 (Boot 3.4.x와 호환되는 버전)
val springCloudVersion = "2024.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        // Spring AI BOM
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
        // [변경 3] Spring Cloud BOM 추가 (필수: 이 부분이 없으면 에러 발생)
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

dependencies {
    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")

    // ta4j
    implementation("org.ta4j:ta4j-core:0.15")

    // batch
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // httpclient5
    implementation("org.apache.httpcomponents.client5:httpclient5")

    // redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.security:spring-security-test")

    // lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // docker
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // db
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")

    // junit
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit-pioneer:junit-pioneer:2.3.0")

    // mockito
    testImplementation("org.mockito:mockito-core:5.18.0")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // p6spy
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.11.0")

    // rest-doc
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.2")

    // swagger-ui
    implementation("org.webjars:swagger-ui:5.10.3")
    implementation("org.webjars:webjars-locator-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openapi3 {
    setServer("http://localhost:8080")
    title = "Stockwellness API"
    description = "자산 배분 시뮬레이터 및 AI 예측 서비스 API 명세서"
    version = "0.0.1"
    format = "yaml"
}

val copyOasToStatic by tasks.registering(Copy::class) {
    dependsOn("openapi3")
    from(layout.buildDirectory.dir("api-spec/openapi3.yaml"))
    into("src/main/resources/static/docs/")
}

tasks.bootJar {
    dependsOn(copyOasToStatic)
}
tasks.bootRun {
    dependsOn(copyOasToStatic)
}