plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    `java-test-fixtures`
}

// core는 라이브러리 형태이므로 실행 가능한 jar를 만들지 않음
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")

    // ta4j
    implementation("org.ta4j:ta4j-core:0.15")

    // httpclient5
    implementation("org.apache.httpcomponents.client5:httpclient5")

    // redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // security (needed for principal and some shared logic)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // db
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")

    // p6spy
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.11.0")

    // querydsl
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
}

// === Test Fixtures Dependencies ===
dependencies {
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesImplementation("org.springframework.security:spring-security-test")
    testFixturesImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
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
tasks.jacocoTestReport {
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
