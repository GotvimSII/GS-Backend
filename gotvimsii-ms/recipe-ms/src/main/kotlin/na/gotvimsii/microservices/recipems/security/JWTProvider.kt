package na.gotvimsii.microservices.recipems.security

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import na.gotvimsii.common.util.Environment
import java.net.URI
import java.util.concurrent.TimeUnit

const val JWT_ISSUER = "JWT_ISSUER"
const val JWT_REALM = "JWT_REALM"
const val AUTH_MS_URL = "AUTH_MS_URL"

const val RECIPE_AUDIENCE = "recipe-ms"

object JWTProvider {
    private val authMsUrl = Environment[AUTH_MS_URL]
    val issuer = Environment[JWT_ISSUER]
    val realm = Environment[JWT_REALM]

    val jwkProvider: JwkProvider = JwkProviderBuilder(URI.create("$authMsUrl/.well-known/jwks.json").toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.HOURS)
        .build()
}