package na.gotvimsii.microservices.recipems

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.types.common.RespVer
import io.ktor.server.application.*
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import na.gotvimsii.common.util.Environment
import kotlin.text.toCharArray

const val RECIPE_REDIS_PASS = "RECIPE_REDIS_PASS"
const val RECIPE_REDIS_USER = "RECIPE_REDIS_USER"

const val LIMIT_REDIS_PASS = "LIMIT_REDIS_PASS"
const val LIMIT_REDIS_USER = "LIMIT_REDIS_USER"

data class AppServices(
    val responseJson: Json,
    val recipeJson: Json,
    val recipeRedis: ReThis,
    val rateLimitRedis: ReThis,
)

fun Application.configureServices() {
    val responseJson = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    val recipeJson = Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val recipeRedis = ReThis(host = "recipe-redis", port = 6379, protocol = RespVer.V3) {
        auth(
            password = Environment[RECIPE_REDIS_PASS].toCharArray(),
            username = Environment[RECIPE_REDIS_USER]
        )
    }

    val rateLimitRedis = ReThis(host = "auth-redis", port = 6379, protocol = RespVer.V3) {
        auth(
            password = Environment[LIMIT_REDIS_PASS].toCharArray(),
            username = Environment[LIMIT_REDIS_USER]
        )
    }

    attributes.put(
        AttributeKey("AppServices"),
        AppServices(responseJson, recipeJson, recipeRedis, rateLimitRedis)
    )
}