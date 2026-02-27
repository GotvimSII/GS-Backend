package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.QueryBuilder

class TsRank(
    private val vector: Expression<*>,
    private val query: Expression<*>
) : Function<Double>(DoubleColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("ts_rank(")
        append(vector)
        append(", ")
        append(query)
        append(')')
    }
}

fun Expression<*>.tsRank(query: Expression<*>) = TsRank(this, query)