package na.gotvimsii.common.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant

object JwtUtils {
    fun verifier(secret: String, issuer: String? = null): JWTVerifier {
        val req = JWT.require(Algorithm.HMAC256(secret))
        if (issuer != null) req.withIssuer(issuer)
        return req.build()
    }

    fun signToken(
        secret: String,
        issuer: String,
        subject: String,
        claims: Map<String, String?>,
        expiresInMs: Long
    ): String {
        val builder = JWT.create()
            .withIssuer(issuer)
            .withSubject(subject)
            .withExpiresAt(Instant.now().plusMillis(expiresInMs))

        claims.forEach { (k, v) ->
            if (v != null) builder.withClaim(k, v)
        }

        return builder.sign(Algorithm.HMAC256(secret))
    }
}