package na.gotvimsii.microservices.authms.security

import na.gotvimsii.common.security.JwtUtils
import na.gotvimsii.common.util.Environment
import java.util.*

const val JWT_SECRET = "JWT_SECRET"
const val JWT_ISSUER = "JWT_ISSUER"
const val JWT_REALM = "KTOR_REALM"

object JwtProvider {
    private val secret = Environment[JWT_SECRET]
    private val issuer = Environment[JWT_ISSUER]
    val realm = Environment[JWT_REALM]

    private const val ACCESS_TOKEN_LIFETIME = 20 * 60 * 1000L // 20 mins
    const val REFRESH_TOKEN_LIFETIME = 30 * 24 * 60 * 60 * 1000L // 30 days

    fun makeAccessToken(userId: UUID): String =
        JwtUtils.signToken(
            secret = secret,
            issuer = issuer,
            subject = "Authentication",
            claims = mapOf(
                "type" to "access",
                "userId" to userId.toString()
            ),
            expiresInMs = ACCESS_TOKEN_LIFETIME
        )

    fun makeRefreshToken(userId: UUID): String =
        JwtUtils.signToken(
            secret = secret,
            issuer = issuer,
            subject = "Authentication",
            claims = mapOf(
                "type" to "refresh",
                "userId" to userId.toString()
            ),
            expiresInMs = REFRESH_TOKEN_LIFETIME
        )

    fun verifier() = JwtUtils.verifier(secret = secret, issuer = issuer)
}