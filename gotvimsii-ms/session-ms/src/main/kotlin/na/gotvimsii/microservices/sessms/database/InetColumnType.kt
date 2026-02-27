package na.gotvimsii.microservices.sessms.database

import org.jetbrains.exposed.v1.core.StringColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

class InetColumnType : StringColumnType() {
    override fun sqlType(): String = "inet"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val parameterValue: PGobject? = value?.let {
            PGobject().apply {
                this.type = sqlType()
                this.value = value as? String
            }
        }
        super.setParameter(stmt, index, parameterValue)
    }

    override fun valueFromDB(value: Any): String = when (value) {
        is String -> value
        else -> error("Cannot convert $value to InetAddress")
    }
}

fun Table.inet(name: String) = registerColumn(name, InetColumnType())