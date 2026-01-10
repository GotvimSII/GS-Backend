package na.gotvimsii.microservices.authms.database

import na.gotvimsii.common.util.citext
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.regexp
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.*

object UserTable : UUIDTable("users") {
    val email = citext("email", 255).uniqueIndex()
        .check { it.regexp("^[a-zA-Z0-9.+]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$") }
    val username = varchar("username", 32).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
}

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UserTable)

    var email by UserTable.email
    var username by UserTable.username
    var passwordHash by UserTable.passwordHash
}