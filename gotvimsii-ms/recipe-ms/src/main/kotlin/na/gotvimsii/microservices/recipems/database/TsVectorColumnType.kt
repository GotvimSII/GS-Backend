package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Table
import org.postgresql.util.PGobject

class TsVectorColumnType : ColumnType<String>(nullable = true) {
    override fun sqlType() = "tsvector"

    override fun valueFromDB(value: Any) = value as String

    override fun valueToDB(value: String?): Any? {
        return PGobject().apply {
            this.type = sqlType()
            this.value = value
        }.value
    }

    override fun notNullValueToDB(value: String): Any {
        return PGobject().apply {
            this.type = sqlType()
            this.value = value
        }
    }

    override fun nonNullValueToString(value: String): String {
        return "'$value'"
    }
}

fun Table.tsvector(name: String) = registerColumn(name, TsVectorColumnType()).databaseGenerated()