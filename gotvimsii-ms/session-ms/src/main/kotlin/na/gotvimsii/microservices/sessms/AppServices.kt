package na.gotvimsii.microservices.sessms

import io.ktor.server.application.Application
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

data class AppServices(
    val json: Json
)

fun Application.configureServices() {
    val json = Json {
        prettyPrint = true
        isLenient = true
    }

    attributes.put(
        AttributeKey("AppServices"),
        AppServices(json)
    )
}