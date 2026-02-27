package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.TextColumnType

class CastToText(
    private val expr: Expression<*>
) : Function<String>(TextColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append(expr)
        append("::text")
    }
}

fun Expression<*>.castToText() = CastToText(this)