rootProject.name = "gotvimsii-backend"

include("common", "gotvimsii-ms:auth-ms", "gotvimsii-ms:session-ms")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
