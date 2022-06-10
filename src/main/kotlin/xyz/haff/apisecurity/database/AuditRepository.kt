package xyz.haff.apisecurity.database

import org.json.JSONArray
import org.json.JSONObject
import java.sql.ResultSet
import java.time.Instant

abstract class AuditRepository {

    abstract fun saveRequest(method: String, path: String, userId: String?): Long
    abstract fun saveResponse(auditId: Long, method: String, path: String, userId: String?, status: Int): Long
    abstract fun list(since: Instant): JSONArray

    // TODO: Am i missing the user ids in the output?
    protected fun rowToJson(row: ResultSet) = JSONObject().apply {
        put("id", row.getLong("audit_id"))
        put("method", row.getString("method"))
        put("path", row.getString("path"))
        put("status", row.getString("status"))
        put("time", row.getTimestamp("audit_time").toInstant())
    }
}