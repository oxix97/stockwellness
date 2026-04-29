plugins {
    java
    id("jacoco")
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("com.epages.restdocs-api-spec") version "0.19.4" apply false
}

allprojects {
    group = "org.stockwellness"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "io.spring.dependency-management")

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

    val springAiVersion = "1.0.0-M1"
    val springCloudVersion = "2024.0.0"

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1")
            mavenBom("org.springframework.ai:spring-ai-bom:1.0.0-M1")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
        }
    }

    dependencies {
        // json
        implementation("org.springframework.boot:spring-boot-starter-json")

        // lombok
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")

        // test
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("org.junit-pioneer:junit-pioneer:2.3.0")
        testImplementation("org.mockito:mockito-core:5.18.0")
    }

    // === JaCoCo 설정 ===
    jacoco {
        toolVersion = "0.8.11"
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        forkEvery = 100 // 주기적으로 JVM 재시작하여 메모리 효율 관리
        finalizedBy(tasks.jacocoTestReport)
    }
}