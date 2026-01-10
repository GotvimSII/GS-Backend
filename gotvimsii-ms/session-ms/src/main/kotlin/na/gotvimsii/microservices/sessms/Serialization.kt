package na.gotvimsii.microservices.sessms

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization(jsonModule: Json) {
    install(ContentNegotiation) {
        json(jsonModule)
    }
}