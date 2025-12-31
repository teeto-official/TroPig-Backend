plugins {
	val kotlinVersion = "1.9.25"

	kotlin("jvm") version kotlinVersion
	kotlin("plugin.spring") version kotlinVersion
	kotlin("plugin.serialization") version kotlinVersion
	kotlin("plugin.jpa") version kotlinVersion

	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
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
//	mavenCentral()
	maven("https://maven-central-asia.storage-download.googleapis.com/maven2/")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database
    implementation("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    
    // swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

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

	// PortOne
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")

	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// Testcontainers
	testImplementation("org.testcontainers:junit-jupiter:1.20.3")
	testImplementation("org.testcontainers:postgresql:1.20.3")

	// MockWebServer
	testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

	// coroutines test (suspend 함수 테스트용)
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

	kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType<Test> {
	useJUnitPlatform()
	
	// 환경 변수를 테스트에 전달
	System.getenv("PORTONE_SECRET_KEY")?.let { 
		environment("PORTONE_SECRET_KEY", it) 
	}
	System.getenv("PORTONE_BASE_URL")?.let { 
		environment("PORTONE_BASE_URL", it) 
	}
	System.getenv("TEST_ACCOUNT_NUMBER")?.let { 
		environment("TEST_ACCOUNT_NUMBER", it) 
	}
	System.getenv("TEST_BIRTHDATE")?.let { 
		environment("TEST_BIRTHDATE", it) 
	}
	
	// 테스트 리포트 생성 설정
	testLogging {
		events("passed", "skipped", "failed")
		showStandardStreams = true  // println 출력을 보기 위해 true로 변경
		showExceptions = true
		showCauses = true
		showStackTraces = false
	}
	
	// 테스트 실패 시에도 계속 진행
	ignoreFailures = false
	
	// 테스트 결과 리포트 생성
	reports {
		junitXml.required.set(true)
		html.required.set(true)
	}
}