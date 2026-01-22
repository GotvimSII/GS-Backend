package na.gotvimsii.microservices.authms.helpers

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.types.common.RespVer
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import na.gotvimsii.common.util.Environment
import kotlin.text.toCharArray

const val REDIS_PASS = "REDIS_PASS"
const val REDIS_USER = "REDIS_USER"

data class AppServices(
    val redis: ReThis,
    val sessionClient: HttpClient,
    val json: Json
)

fun Application.configureServices() {
    val json = Json {
        prettyPrint = true
        isLenient = true
    }

    val sessionClient = HttpClient(Java) {
        defaultRequest {
            url("http://session-ms:7241")
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    val redis = ReThis(host = "auth-redis", port = 6379, protocol = RespVer.V3) {
        auth(
            password = Environment[REDIS_PASS].toCharArray(),
            username = Environment[REDIS_USER]
        )
    }

    attributes.put(
        AttributeKey("AppServices"),
        AppServices(redis, sessionClient, json)
    )
}
