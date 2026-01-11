plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.serialization") version "1.9.22"
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jetbrains.kotlin.plugin.jpa") version "1.9.0"
}

group = "com.tropig.backend"
version = "0.0.1-SNAPSHOT"
description = "TRoPiG Backend Project"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Database
    implementation("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    
    // swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

	// Cache
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("com.github.ben-manes.caffeine:caffeine")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
	implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")

	implementation("org.springframework.boot:spring-boot-starter-security")

	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25") // or "kotlin-stdlib-jdk8"
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0") // JVM dependency

	// EMAIL
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// AWS S3
	implementation(platform("software.amazon.awssdk:bom:2.20.0"))
	implementation("software.amazon.awssdk:s3")

	implementation("com.google.code.gson:gson:2.10.1")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	
	// 테스트 리포트 생성 설정
	testLogging {
		events("passed", "skipped", "failed")
		showStandardStreams = false
	}
	
	// 테스트 실패 시에도 계속 진행
	ignoreFailures = false
	
	// 테스트 결과 리포트 생성
	reports {
		junitXml.required.set(true)
		html.required.set(true)
	}
}