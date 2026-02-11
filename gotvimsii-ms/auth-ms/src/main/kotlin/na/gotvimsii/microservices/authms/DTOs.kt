@file:UseSerializers(serializerClasses = [UUIDSerializer::class, InstantSerializer::class])

package na.gotvimsii.microservices.authms

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import na.gotvimsii.common.classes.serializers.InstantSerializer
import na.gotvimsii.common.classes.serializers.UUIDSerializer
import java.time.Instant
import java.util.*

@Serializable
data class PublicKeys(
    val keys: List<Map<String, String>>
)

@Serializable
data class RegistrationCredentials(
    val email: String,
    val username: String,
    val password: String
)

@Serializable
data class LoginCredentials(
    val email: String,
    val username: String,
    val password: String
)

@Serializable
data class RedisSessionEntry(
    val sessionId: UUID,
    val userId: UUID,
    val expiresAt: Instant
)

@Serializable
data class RefreshTokenHash(
    val refreshTokenHash: String
)