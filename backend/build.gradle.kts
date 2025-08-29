import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("kapt") version "1.9.23"
}

group = "com.back"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// 코틀린 컴파일러 설정
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

// KAPT 설정 (Lombok 처리용)
kapt {
    keepJavacAnnotationProcessors = true
    showProcessorStats = true
    arguments {
        arg("lombok.addLombokGeneratedAnnotation", "true")
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

dependencies {
    // Kotlin 표준 라이브러리
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    
    // 기타 라이브러리
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // JWT 런타임
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Lombok (마이그레이션 기간 동안만 유지)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    kapt("org.projectlombok:lombok:1.18.34")

    // 테스트 Lombok
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

    // 데이터베이스
    runtimeOnly("com.h2database:h2")
    implementation("org.postgresql:postgresql")

    // 개발 도구
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

// 소스 디렉토리 설정 (기본 Gradle 설정 사용)
sourceSets {
    main {
        java {
            srcDirs("src/main/java")
        }
        kotlin {
            srcDirs("src/main/kotlin")
        }
    }
    test {
        java {
            srcDirs("src/test/java")
        }
        kotlin {
            srcDirs("src/test/kotlin")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val domains = listOf("user", "exchange", "trade_log", "wallet", "coin")
domains.forEach { d ->
    tasks.register<Test>("test${d.replaceFirstChar { it.uppercase() }}") {
        group = "verification"
        description = "Runs $d domain tests"
        useJUnitPlatform {
            includeTags(d)
        }
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        shouldRunAfter(tasks["test"])
    }
}

sourceSets.create("integrationTest") {
    java.srcDir("src/integrationTest/java")
    resources.srcDir("src/integrationTest/resources")
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs integration tests (실제 API 호출 등)"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks["test"])
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    val failed = mutableListOf<Pair<TestDescriptor, TestResult>>()
    val taskName = name

    testLogging {
        events("FAILED")
        exceptionFormat = TestExceptionFormat.SHORT
        showCauses = true
        showStackTraces = true
        showStandardStreams = false
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            if (result.resultType == TestResult.ResultType.FAILURE) {
                failed += testDescriptor to result
            }
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                println("── $taskName summary ───────────────────────────")
                println("Result  : ${result.resultType}")
                println("Tests   : ${result.testCount},  Failed : ${result.failedTestCount}")
                println("──────────────────────────────────────────────")

                if (failed.isNotEmpty()) {
                    println("❌ Failed tests (${failed.size}):")
                    failed.forEach { (d, r) ->
                        val ex = r.exceptions.firstOrNull()
                        println(" - ${d.className}.${d.name}")
                        if (ex != null) {
                            println("   ${ex.javaClass.simpleName}: ${ex.message}")
                        }
                    }
                    println("──────────────────────────────────────────────")
                }
            }
        }
    })
}