plugins {
    java
    id("jacoco")
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.epages.restdocs-api-spec") version "0.19.4"
}

group = "org"
version = "0.0.1-SNAPSHOT"
description = "stockwellness"

val springAiVersion = "1.0.0-M1"
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
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
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
    testImplementation("org.springframework.batch:spring-batch-test")

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
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

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

// === QueryDSL 빌드 옵션 ===
val querydslDir = layout.buildDirectory.dir("generated/querydsl")

sourceSets {
    main {
        java {
            srcDir(querydslDir)
        }
    }
}

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(querydslDir)
}

tasks.named("clean") {
    doLast {
        delete(querydslDir)
    }
}

// === JaCoCo 설정 ===
jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/Q*",
                    "**/*Application*",
                    "**/*Config*",
                    "**/*Dto*",
                    "**/*Request*",
                    "**/*Response*",
                    "**/*Exception*",
                    "**/global/security/jwt/**"
                )
            }
        })
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// === OpenAPI 설정 ===
openapi3 {
    setServer("http://localhost:8080")
    title = "Stockwellness API"
    description = "자산 배분 시뮬레이터 및 AI 예측 서비스 API 명세서"
    version = "0.0.1"
    format = "yaml"
}

// openapi3 태스크의 기본 의존성 재설정
afterEvaluate {
    tasks.named("openapi3") {
        setDependsOn(listOf(tasks.test))
    }
}

// jar 태스크 비활성화
tasks.jar {
    enabled = false
}

// 소스 디렉토리의 openapi3.yaml을 최신으로 업데이트
tasks.register<Copy>("updateOpenApiSpec") {
    dependsOn("openapi3")
    from(layout.buildDirectory.file("api-spec/openapi3.yaml"))
    into("src/main/resources/static/docs")
}

// build 실행 시 openapi3.yaml 자동 업데이트
tasks.build {
    dependsOn("updateOpenApiSpec")
}

// bootRun 실행 시에도 최신 문서로 업데이트
tasks.bootRun {
    dependsOn("updateOpenApiSpec")
}
