plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.epages.restdocs-api-spec")
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
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // kafka
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("com.h2database:h2")

    // security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // rest-doc
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.2")

    // swagger-ui
    implementation("org.webjars:swagger-ui:5.10.3")
    implementation("org.webjars:webjars-locator-core")
    
    // docker
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
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
tasks.matching { it.name == "openapi3" }.configureEach {
    // 테스트 결과물(snippets)이 생성된 후에 실행 가능
    mustRunAfter(tasks.test)
}

// 소스 디렉토리의 openapi3.yaml을 수동으로 업데이트할 때 사용하는 태스크
// 사용법: ./gradlew updateOpenApiSpec
tasks.register<Copy>("updateOpenApiSpec") {
    group = "documentation"
    description = "테스트 기반 snippets를 사용하여 src/main/resources에 OpenAPI 스펙을 업데이트합니다."
    
    dependsOn(tasks.test)
    dependsOn(tasks.matching { it.name == "openapi3" })
    
    from(layout.buildDirectory.file("api-spec/openapi3.yaml"))
    into("src/main/resources/static/docs")
}
