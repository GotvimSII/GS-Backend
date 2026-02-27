package na.gotvimsii.microservices.authms.security

import na.gotvimsii.common.security.JWTUtils
import na.gotvimsii.common.util.Environment
import java.util.*

const val JWT_ISSUER = "JWT_ISSUER"
const val JWT_REALM = "JWT_REALM"

object JWTProvider {
    private val issuer = Environment[JWT_ISSUER]
    val realm = Environment[JWT_REALM]

    private const val ACCESS_TOKEN_LIFETIME = 20 * 60 * 1000L // 20 mins
    const val REFRESH_TOKEN_LIFETIME = 30 * 24 * 60 * 60 * 1000L // 30 days

    private const val AUTH_AUDIENCE = "auth-ms"
    private const val RECIPE_AUDIENCE = "recipe-ms"

    fun makeAccessToken(userId: UUID): String =
        JWTUtils.signToken(
            privateKey = ECKeyProvider.privateKey,
            publicKey = ECKeyProvider.publicKey,
            issuer = issuer,
            subject = "Authentication",
            claims = mapOf(
                "type" to "access",
                "userId" to userId.toString()
            ),
            expiresInMs = ACCESS_TOKEN_LIFETIME,
            audiences = arrayOf(AUTH_AUDIENCE, RECIPE_AUDIENCE)
        )

    fun makeRefreshToken(userId: UUID): String =
        JWTUtils.signToken(
            privateKey = ECKeyProvider.privateKey,
            publicKey = ECKeyProvider.publicKey,
            issuer = issuer,
            subject = "Authentication",
            claims = mapOf(
                "type" to "refresh",
                "userId" to userId.toString()
            ),
            expiresInMs = REFRESH_TOKEN_LIFETIME,
            audiences = arrayOf(AUTH_AUDIENCE)
        )

    fun accessTokenVerifier() = JWTUtils.verifier(
        publicKey = ECKeyProvider.publicKey,
        issuer = issuer,
        audiences = arrayOf(AUTH_AUDIENCE)
    )

    fun refreshTokenVerifier() = JWTUtils.verifier(
        publicKey = ECKeyProvider.publicKey,
        issuer = issuer,
        audiences = arrayOf(AUTH_AUDIENCE)
    )
}