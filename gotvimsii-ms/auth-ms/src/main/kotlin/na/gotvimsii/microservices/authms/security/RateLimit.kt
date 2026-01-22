package na.gotvimsii.microservices.authms.security

import eu.vendeli.rethis.command.generic.expire
import eu.vendeli.rethis.command.string.incr
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import na.gotvimsii.common.classes.ApiError
import na.gotvimsii.microservices.authms.helpers.clientIp
import na.gotvimsii.microservices.authms.services
import kotlin.time.Duration.Companion.seconds

class RateLimitConfig {
    var requests: Long = 60
    var windowSeconds: Long = 60
    var keySelector: (ApplicationCall) -> String = { call ->
        call.clientIp()
    }
}

val RedisRateLimit = createRouteScopedPlugin(
    name = "RedisRateLimit",
    createConfiguration = ::RateLimitConfig
) {
    val redis = application.services.redis

    onCall { call ->
        val keyBase = pluginConfig.keySelector(call)
        val path = call.request.path()
        val redisKey = "ratelimit:$path:$keyBase"

        val results = redis.transaction {
            incr(redisKey)
            expire(redisKey, pluginConfig.windowSeconds.seconds)
        }

        val count = (results?.first()?.value as Number).toLong()

        if (count > pluginConfig.requests) {
            call.response.header(
                HttpHeaders.RetryAfter,
                pluginConfig.windowSeconds
            )

            call.respond(
                HttpStatusCode.TooManyRequests,
                ApiError("Too many requests.")
            )

            return@onCall
        }
    }
}