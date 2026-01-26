plugins {
    java
    id("jacoco")
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

    // querydsl
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
}

// JaCoCo 설정
jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // 리포트 생성 전 테스트 실행 강제
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    // 커버리지 측정 제외 대상 설정
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/Q*",                         // QueryDSL 생성 클래스
                    "**/*Application*",              // 메인 애플리케이션
                    "**/*Config*",                   // 설정 파일
                    "**/*Dto*",                      // DTO
                    "**/*Request*",                  // Request 객체
                    "**/*Response*",                 // Response 객체
                    "**/*Exception*",                // 예외 클래스
                    "**/global/security/jwt/**"      // 보안 관련 라이브러리성 코드
                )
            }
        })
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // 테스트 후 리포트 자동 생성
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
