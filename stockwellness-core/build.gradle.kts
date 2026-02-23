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

// build/generated/querydsl 경로 설정
val generatedDir = layout.buildDirectory.dir("generated/querydsl").get().asFile

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
}
// 3. JavaCompile 태스크 설정: Q클래스가 생성될 디렉토리 지정
tasks.withType<JavaCompile>().configureEach {
    options.generatedSourceOutputDirectory.set(generatedDir)
}

// 4. 생성된 Q클래스를 IDE가 소스코드로 인식할 수 있도록 sourceSets 추가
sourceSets {
    main {
        java.srcDirs(generatedDir)
    }
}

// 5. ./gradlew clean 실행 시 생성된 Q클래스 폴더도 함께 삭제
tasks.named<Delete>("clean") {
    delete(generatedDir)
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
