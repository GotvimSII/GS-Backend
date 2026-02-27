package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.QueryBuilder

class NullIf<T>(
    private val expr: ExpressionWithColumnType<T>,
    private val other: ExpressionWithColumnType<T>
) : Function<T>(expr.columnType) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("nullif(")
        append(expr)
        append(", ")
        append(other)
        append(')')
    }
}

fun <T> ExpressionWithColumnType<T>.nullIf(other: ExpressionWithColumnType<T>) = NullIf(this, other)