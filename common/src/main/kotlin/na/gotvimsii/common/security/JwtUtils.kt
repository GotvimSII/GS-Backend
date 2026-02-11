package na.gotvimsii.common.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.math.BigInteger
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.time.Instant
import java.util.*

object JwtUtils {
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

fun ECPublicKey.toJWK(kid: String) = mapOf(
    "kty" to "EC",
    "use" to "sig",
    "alg" to "ES256",
    "crv" to "P-256",
    "kid" to kid,
    "x" to base64UrlWUnsigned(w.affineX),
    "y" to base64UrlWUnsigned(w.affineY)
)

private fun base64UrlWUnsigned(value: BigInteger): String =
    Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray().stripLeadingZero())

private fun ByteArray.stripLeadingZero(): ByteArray =
    if (size > 1 && this[0] == 0.toByte()) copyOfRange(1, size) else this