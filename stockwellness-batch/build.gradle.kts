plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":stockwellness-core"))
    testImplementation(testFixtures(project(":stockwellness-core")))

    // web (for BatchAdminController)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // jpa (for EntityManagerFactory in Batch Configs)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // batch
    implementation("org.springframework.boot:spring-boot-starter-batch")
    testImplementation("org.springframework.batch:spring-batch-test")
    testRuntimeOnly("com.h2database:h2")
}
