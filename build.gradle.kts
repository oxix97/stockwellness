plugins {
    java
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
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
}

tasks.withType<Test> {
    useJUnitPlatform()
}

