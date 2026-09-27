plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    // -PuseMavenLocal builds against a yaft-java version that is not released
    // yet (publishToMavenLocal in yaft-java). CI never sets it: this
    // repository exists to prove what Maven Central actually serves.
    if (providers.gradleProperty("useMavenLocal").isPresent) {
        mavenLocal()
    }
    mavenCentral()
}

val e2eTest: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[e2eTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[e2eTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(libs.yaft)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
}

// Against a running, seeded backend: scripts/backend.sh up && scripts/seed.sh.
// Not part of check, because it needs Docker and a seed.
val e2e = tasks.register<Test>("e2eTest") {
    description = "Runs the playground against a real YaFT backend."
    group = "verification"
    testClassesDirs = e2eTest.output.classesDirs
    classpath = e2eTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
    // The seed changes on every run; never serve a cached result.
    outputs.upToDateWhen { false }
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
