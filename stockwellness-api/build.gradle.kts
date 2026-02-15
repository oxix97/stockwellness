plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.epages.restdocs-api-spec")
}

dependencies {
    implementation(project(":stockwellness-core"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.security:spring-security-test")

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
afterEvaluate {
    tasks.named("openapi3") {
        setDependsOn(listOf(tasks.test))
    }
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
