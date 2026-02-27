package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.Function

class StringAgg(
    private val expr: Expression<String>,
    private val delimiter: String
) : Function<String>(TextColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("string_agg(")
        append(expr)
        append(", ")
        append(stringLiteral(delimiter))
        append(')')
    }
}


fun Expression<String>.stringAgg(delimiter: String) = StringAgg(this, delimiter)