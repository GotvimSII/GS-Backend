@file:UseSerializers(serializerClasses = [UUIDSerializer::class])

package na.gotvimsii.common.classes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import na.gotvimsii.common.classes.serializers.UUIDSerializer
import java.util.*

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
data class UserPrincipal(val userId: UUID)