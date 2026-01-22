package na.gotvimsii.microservices.authms.helpers

import io.ktor.server.application.*
import io.ktor.server.plugins.*

fun ApplicationCall.clientIp(): String {
    val isBehindProxy = request.origin.remoteAddress.startsWith("172.")
            || request.origin.remoteAddress.startsWith("10.")
            || request.origin.remoteAddress.startsWith("192.168")

    if (isBehindProxy) {
        request.headers["CF-Connecting-IP"]?.let { return it }
        request.headers["X-Forwarded-For"]?.let { return it }
    }

    return request.origin.remoteAddress
}