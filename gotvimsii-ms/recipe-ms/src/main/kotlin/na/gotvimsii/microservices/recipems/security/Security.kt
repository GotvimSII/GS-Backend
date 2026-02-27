package na.gotvimsii.microservices.recipems.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import na.gotvimsii.common.classes.ApiError

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JWTProvider.realm
            verifier(JWTProvider.jwkProvider, JWTProvider.issuer) {
                withAudience(RECIPE_AUDIENCE)
            }
            validate { credential ->
                val tokenType = credential.payload.claims["type"]?.asString()

                if (tokenType == "access") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("Invalid or expired token!")
                )
            }
        }
    }
}