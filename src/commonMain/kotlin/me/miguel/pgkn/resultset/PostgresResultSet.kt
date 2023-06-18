package me.miguel.pgkn.resultset

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toKString
import kotlinx.datetime.*
import libpq.*
import me.miguel.pgkn.SQLException

/**
 * To Fix ISO 8601, as postgres default is space not "T"
 * https://www.postgresql.org/docs/current/datatype-datetime.html#DATATYPE-DATETIME-OUTPUT
 */
private fun String.fixIso8601() = replaceRange(10, 11, "T")

internal class PostgresResultSet(val internal: CPointer<PGresult>) : ResultSet {

    private val rowCount: Int = PQntuples(internal)
    private val columnCount: Int = PQnfields(internal)

    private var currentRow = -1


    override fun next(): Boolean {
        if (currentRow > rowCount - 2) {
            return false
        }
        currentRow++
        return true
    }

    private fun isNull(columnIndex: Int): Boolean =
        PQgetisnull(res = internal, tup_num = currentRow, field_num = columnIndex) == 1

    private fun getPointer(columnIndex: Int): CPointer<ByteVar>? {
        if (isNull(columnIndex)) return null
        return PQgetvalue(res = internal, tup_num = currentRow, field_num = columnIndex) ?: throw SQLException()
    }


    /**
     * Are all non-binary columns returned as text?
     * https://www.postgresql.org/docs/9.5/libpq-exec.html#LIBPQ-EXEC-SELECT-INFO
     */
    override fun getString(columnIndex: Int): String? = getPointer(columnIndex)?.toKString().also { println(it) }

    override fun getBoolean(columnIndex: Int): Boolean? = getString(columnIndex)?.equals("t")

    override fun getShort(columnIndex: Int): Short? = getString(columnIndex)?.toShort()


    override fun getInt(columnIndex: Int): Int? = getString(columnIndex)?.toInt()


    override fun getLong(columnIndex: Int): Long? = getString(columnIndex)?.toLong()


    override fun getFloat(columnIndex: Int): Float? = getString(columnIndex)?.toFloat()

    override fun getDouble(columnIndex: Int): Double? = getString(columnIndex)?.toDouble()

    override fun getBytes(columnIndex: Int): ByteArray? = getString(columnIndex)?.encodeToByteArray()

    override fun getDate(columnIndex: Int): LocalDate? = getString(columnIndex)?.toLocalDate()

    override fun getTime(columnIndex: Int): LocalTime? = getString(columnIndex)?.toLocalTime()
    override fun getLocalDateTime(columnIndex: Int): LocalDateTime? = getString(columnIndex)
        ?.fixIso8601()
        ?.toLocalDateTime()

    override fun getInstant(columnIndex: Int): Instant? = getString(columnIndex)
        ?.fixIso8601()
        ?.toInstant()
}
