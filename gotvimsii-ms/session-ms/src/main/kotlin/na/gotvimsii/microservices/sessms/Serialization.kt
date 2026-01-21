package na.gotvimsii.microservices.sessms

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    val json = this.services.json
    install(ContentNegotiation) {
        json(json)
    }
}