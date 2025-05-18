plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

kotlin {

    jvmToolchain(23)
}


repositories { mavenCentral() }

dependencies {
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-websockets:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")

    implementation("org.slf4j:slf4j-simple:1.7.36")
}

application { mainClass.set("MainKt") }


tasks.test {
    useJUnitPlatform()
}