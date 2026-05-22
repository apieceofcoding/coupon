buildscript {
	dependencies {
		classpath("org.ow2.asm:asm:9.8")
	}
}

plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.3.21"
	id("com.google.cloud.tools.jib") version "3.4.4"
}

group = "com.apiece"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
	// 4-2 매진 시그널의 Application fast-path 용. JVM 안에 1 초 TTL in-process 캐시를 두어
	// Redis EXISTS 호출도 인스턴스 당 초당 1회로 줄인다.
	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
	runtimeOnly("com.mysql:mysql-connector-j")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.13")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

jib {
	from {
		// docker:// 는 jib 가 로컬 docker daemon 의 이미지를 가져오게 한다. registry
		// 접근이 없으니 docker-credential-helper 가 hub credential 을 찾다가 띄우는
		// 4 줄짜리 noise 가 사라진다. @sha256:... digest 명시는 reproducibility 보장
		// 과 동시에 "image digest 없음" warning 도 제거.
		// 사전 준비 (학생이 한 번만): `docker pull eclipse-temurin:25-jre`.
		image = "docker://eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc"
		platforms {
			platform {
				architecture = "arm64"
				os = "linux"
			}
		}
	}
	to {
		image = "coupon-service"
		tags = setOf("latest", project.version.toString())
	}
	container {
		ports = listOf("8080")
		creationTime.set("USE_CURRENT_TIMESTAMP")
	}
}
