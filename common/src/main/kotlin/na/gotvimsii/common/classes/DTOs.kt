@file:UseSerializers(serializerClasses = [UUIDSerializer::class])

package na.gotvimsii.common.classes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import na.gotvimsii.common.classes.serializers.UUIDSerializer
import java.util.*

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("session_id") val sessionId: UUID
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("session_id") val sessionId: UUID
)

@Serializable
data class RefreshResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class SessionID(val id: UUID)

@Serializable
data class ApiError(
    @SerialName("error") val message: String
)

@Serializable
data class ApiSuccess(
    val message: String
)

@Serializable
data class AuthUser(val userId: UUID)