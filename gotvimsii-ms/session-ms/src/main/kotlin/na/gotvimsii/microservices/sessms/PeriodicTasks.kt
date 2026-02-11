package na.gotvimsii.microservices.sessms

import dev.nikdi.periodicworker.PeriodicWorker
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import na.gotvimsii.microservices.sessms.database.SessionTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

fun Application.configurePeriodicTasks() {
    install(PeriodicWorker) {
        every(24.hours, true, Dispatchers.IO) {
            val expiredEntries = transaction {
                SessionTable.update(where = {
                    (SessionTable.expiresAt less Instant.now()) and (SessionTable.isRevoked eq false)
                }) { record ->
                    record[isRevoked] = true
                    record[updatedAt] = Instant.now()
                }
            }
            this@configurePeriodicTasks.log.info("Updated $expiredEntries expired sessions. ")
        }

        every(5.days, true, Dispatchers.IO) {
            val deletedEntries = transaction {
                SessionTable.deleteWhere {
                    (SessionTable.isRevoked eq true) and
                            (SessionTable.updatedAt less Instant.now().minusSeconds(1.days.inWholeSeconds))
                }
            }
            this@configurePeriodicTasks.log.info("Deleted $deletedEntries revoked sessions.")
        }
    }
}