package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder

class MatchesTsQuery(
    private val vector: Expression<*>,
    private val query: Expression<*>
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append(vector)
        append(" @@ ")
        append(query)
    }
}

fun Expression<*>.matches(query: Expression<*>) = MatchesTsQuery(this, query)