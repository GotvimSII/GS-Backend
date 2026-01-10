package na.gotvimsii.common.util

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

private class CitextColumnType(
    colLength: Int
) : VarCharColumnType(colLength) {
    override fun sqlType(): String = "CITEXT"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val parameterValue: PGobject = value.let {
            PGobject().apply {
                type = sqlType()
                this.value = value as? String
            }
        }
        super.setParameter(stmt, index, parameterValue)
    }
}

fun Table.citext(name: String, length: Int): Column<String> = registerColumn(name, CitextColumnType(length))