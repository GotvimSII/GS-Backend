package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.TextColumnType

class PgConcat(
    private vararg val parts: Expression<String>
) : Function<String>(TextColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        parts.forEachIndexed { index, expression ->
            append(expression)
            if (index < parts.lastIndex) append(" || ")
        }
    }
}

fun concat(vararg parts: Expression<String>) = PgConcat(*parts)