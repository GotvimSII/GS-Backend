plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "na.gotvimsii"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.dotenv.kotlin)
    implementation(libs.postgresql)
    implementation(libs.exposed.core)
    testImplementation(libs.kotlin.test.junit)
}