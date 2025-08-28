import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
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
    // 코틀린 표준 라이브러리
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // --- 기존 의존성 ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.postgresql:postgresql")
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "src/main/kotlin")
        }
        kotlin {
            srcDirs("src/main/kotlin")
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