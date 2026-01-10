package na.gotvimsii.microservices.sessms.database

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object SessionTable : UUIDTable("sessions") {
    val userId = uuid("user_id")
    val refreshTokenHash = binary("refresh_token_hash").uniqueIndex("session_unique_hash")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    val expiresAt = timestamp("expires_at")
    val ipAddress = inet("ip_address").nullable()
    val userAgent = text("user_agent").nullable()
    val isRevoked = bool("is_revoked").default(false)
}