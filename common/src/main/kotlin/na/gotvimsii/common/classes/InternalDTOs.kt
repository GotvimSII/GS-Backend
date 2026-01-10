@file:UseSerializers(serializerClasses = [UUIDSerializer::class, InstantSerializer::class])

package na.gotvimsii.common.classes

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import na.gotvimsii.common.classes.serializers.InstantSerializer
import na.gotvimsii.common.classes.serializers.UUIDSerializer
import java.time.Instant
import java.util.*

//@Serializable
//data class TokenIntrospectResponse(
//    val active: Boolean,
//    val payload: TokenPayload? = null
//)
//
//@Serializable
//data class TokenPayload(
//    val sub: String,
//    val userId: String,
//    val exp: Long
//) leaving them in for later usage

@Serializable
data class NewSessionRequest(
    val userId: UUID,
    val refreshTokenHash: String,
    val expiresAt: Instant,
    val ipAddress: String,
    val userAgent: String?
)

@Serializable
data class UpdateSessionRequest(
    val refreshTokenHash: String,
    val expiresAt: Instant,
    val ipAddress: String,
    val userAgent: String?
)