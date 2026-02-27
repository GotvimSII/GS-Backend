package na.gotvimsii.common.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.time.Instant

object JWTUtils {
    fun verifier(
        publicKey: ECPublicKey,
        issuer: String? = null,
        vararg audiences: String? = emptyArray()
    ): JWTVerifier {
        val req = JWT.require(Algorithm.ECDSA256(publicKey, null))
        if (issuer != null) req.withIssuer(issuer)
        if (audiences.isNotEmpty()) req.withAudience(*audiences)
        return req.build()
    }

    fun signToken(
        privateKey: ECPrivateKey,
        publicKey: ECPublicKey,
        issuer: String,
        subject: String,
        claims: Map<String, String?>,
        expiresInMs: Long,
        vararg audiences: String? = emptyArray(),
    ): String {
        val now = Instant.now()

        val builder = JWT.create()
            .withIssuer(issuer)
            .withSubject(subject)
            .withIssuedAt(now)
            .withExpiresAt(now.plusMillis(expiresInMs))

        claims.forEach { (k, v) ->
            if (v != null) builder.withClaim(k, v)
        }

        if (audiences.isNotEmpty()) builder.withAudience(*audiences)

        return builder.sign(
            Algorithm.ECDSA256(
                publicKey,
                privateKey
            )
        )
    }
}