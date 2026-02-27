package na.gotvimsii.microservices.recipems.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import na.gotvimsii.common.util.Environment
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

const val DB_URL = "DB_URL"
const val DB_USER = "POSTGRES_USER"
const val DB_PASS = "POSTGRES_PASSWORD"

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    fun init(migrate: Boolean = false) {
        val config = HikariConfig().apply {
            jdbcUrl = Environment[DB_URL]
            username = Environment[DB_USER]
            password = Environment[DB_PASS]
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
        }

        dataSource = HikariDataSource(config)

        Flyway.configure()
            .driver(config.driverClassName)
            .dataSource(this@DatabaseFactory.dataSource)
            .skipDefaultCallbacks(true)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .baselineOnMigrate(true)
            .load()
            .also { if (migrate) it.migrate() }

        Database.connect(dataSource)
    }

    fun close() {
        if (this::dataSource.isInitialized) dataSource.close()
    }
}