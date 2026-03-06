package na.gotvimsii.microservices.recipems

import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.*

fun Application.configureReverseProxy() {
    install(XForwardedHeaders) {
        useFirstProxy()
    }
}