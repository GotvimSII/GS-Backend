package na.gotvimsii.microservices.authms.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import na.gotvimsii.common.classes.ApiError
import na.gotvimsii.common.classes.AuthUser
import java.util.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtProvider.realm
            verifier(JwtProvider.accessTokenVerifier())
            validate { credential ->
                val userId = credential.payload.claims["userId"]?.asString()?.let(UUID::fromString)
                userId?.let { AuthUser(it) }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("Missing or invalid token.")
                )
            }
        }
    }
}
