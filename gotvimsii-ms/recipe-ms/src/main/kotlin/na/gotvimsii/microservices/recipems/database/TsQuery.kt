package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.Function

class TsQuery(
    private val config: String,
    private val query: Expression<String>
) : Function<String>(TextColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("plainto_tsquery(")
        append(stringLiteral(config))
        append(", ")
        append(query)
        append(')')
    }
}

fun plainToTsQuery(config: String, query: String) = TsQuery(config, stringLiteral(query))