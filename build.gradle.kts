plugins {
    java
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.epages.restdocs-api-spec") version "0.19.2"
}

group = "org"
version = "0.0.1-SNAPSHOT"
description = "stockwellness"

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
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    //ta4j
    implementation("org.ta4j:ta4j-core:0.15")
    //batch
    implementation("org.springframework.boot:spring-boot-starter-batch")
    //httpclient5
    implementation("org.apache.httpcomponents.client5:httpclient5")
    //redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    //jwt
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    //jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    //security
    implementation("org.springframework.boot:spring-boot-starter-security")
    //validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    //web
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.security:spring-security-test")
    //lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    //docker
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    //db
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")
    //junit
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit-pioneer:junit-pioneer:2.3.0")
    //mockito
    testImplementation("org.mockito:mockito-core:5.18.0")
    //test
    testImplementation("org.springframework.boot:spring-boot-starter-test")  // webmvc-test 대신 기본 test 사용 (충분함)
    //p6spy
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.11.0")
    //rest-doc
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.2")
    //swagger-ui
    implementation("org.webjars:swagger-ui:5.10.3")
    implementation("org.webjars:webjars-locator-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// OpenAPI 3 명세 생성 설정
openapi3 {
    setServer("http://localhost:8080")
    title = "Stockwellness API"
    description = "자산 배분 시뮬레이터 및 AI 예측 서비스 API 명세서"
    version = "0.0.1"
    format = "yaml"
}

// 생성된 문서를 Spring Boot가 서빙할 수 있는 경로로 복사
val copyOasToStatic by tasks.registering(Copy::class) {
    dependsOn("openapi3") // openapi3 태스크가 먼저 실행되어야 함

    // build/api-spec/openapi3.yaml -> src/main/resources/static/docs/openapi3.yaml
    from(layout.buildDirectory.dir("api-spec/openapi3.yaml"))
    into("src/main/resources/static/docs/")
}

// bootJar 실행 전 문서 생성 및 복사 강제
tasks.bootJar {
    dependsOn(copyOasToStatic)
}
tasks.bootRun {
    dependsOn(copyOasToStatic)
}
