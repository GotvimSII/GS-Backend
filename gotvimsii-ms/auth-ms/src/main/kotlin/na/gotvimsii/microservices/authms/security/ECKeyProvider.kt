package na.gotvimsii.microservices.authms.security

import na.gotvimsii.common.security.SelfSignedCertGenerator
import na.gotvimsii.common.util.Environment
import na.gotvimsii.microservices.authms.PublicKeys
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

// TODO make the file writes atomic

object ECKeyProvider {
    private val password = Environment["KS_PASSWORD"].toCharArray()
    private val keyStorePath = Path("keystore.p12")

    private val keyStore: KeyStore = KeyStore.getInstance("pkcs12").apply {
        if (keyStorePath.exists()) {
            keyStorePath.inputStream().use { stream ->
                load(stream, password)
            }
        } else {
            load(null, password)
            keyStorePath.outputStream().use { stream ->
                store(stream, password)
            }
        }
    }

    val keyPair: KeyPair by lazy {
        val alias = "ec-${Instant.now()}"

        val existing = keyStore.getEntry(
            alias,
            KeyStore.PasswordProtection(password)
        ) as? KeyStore.PrivateKeyEntry

        if (existing != null) {
            KeyPair(existing.certificate.publicKey, existing.privateKey)
        } else {
            val pair = generateKeyPair()
            storeKeyPair(alias, pair)
            pair
        }
    }


    fun buildJwks(): PublicKeys {
        val keys = keyStore.aliases().toList().mapNotNull { alias ->
            val cert = keyStore.getCertificate(alias) ?: return@mapNotNull null
            val pub = cert.publicKey as? ECPublicKey ?: return@mapNotNull null

            ecPublicKeyToJwk(alias, pub)
        }

        return PublicKeys(keys = keys)
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }

    private fun storeKeyPair(alias: String, keyPair: KeyPair) {
        val cert = SelfSignedCertGenerator.generate(keyPair)

        keyStore.setEntry(
            alias,
            KeyStore.PrivateKeyEntry(keyPair.private, arrayOf(cert)),
            KeyStore.PasswordProtection(password)
        )

        keyStorePath.outputStream().use { stream ->
            keyStore.store(stream, password)
        }
    }

    val privateKey get() = keyPair.private as ECPrivateKey
    val publicKey get() = keyPair.public as ECPublicKey
}

fun ecPublicKeyToJwk(kid: String, key: ECPublicKey): Map<String, String> {
    val point = key.w
    val x = base64Url(point.affineX)
    val y = base64Url(point.affineY)

    return mapOf(
        "kty" to "EC",
        "crv" to "P-256",
        "use" to "sig",
        "alg" to "ES256",
        "kid" to kid,
        "x" to x,
        "y" to y
    )
}

fun base64Url(value: BigInteger): String {
    val bytes = value.toByteArray().let {
        if (it[0] == 0.toByte()) it.drop(1).toByteArray() else it
    }

    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(bytes)
}