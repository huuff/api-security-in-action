package xyz.haff.apisecurity.controller

import org.dalesbred.Database
import org.json.JSONArray
import org.json.JSONObject
import spark.Request
import spark.Response
import xyz.haff.apisecurity.Config
import java.sql.ResultSet
import java.time.Instant
import java.time.temporal.ChronoUnit

// TODO: Test it
// TODO: Repositories for safe/unsafe
class AuditController(
    private val database: Database,
    private val config: Config,
) {

    fun auditRequestStart(request: Request, response: Response) = database.withVoidTransaction {
        val auditId = database.findUniqueLong("SELECT NEXT VALUE FOR audit_id_seq")
        request.attribute("audit_id", auditId)
        database.updateUnique("""
            INSERT INTO audit_log(audit_id, method, path, user_id, audit_time)
                VALUES (?, ?, ?, ?, current_timestamp)
        """, auditId, request.requestMethod(), request.pathInfo(), request.attribute("subject")
        )
    }

    fun auditRequestEnd(request: Request, response: Response) = database.withVoidTransaction {
        database.updateUnique("""
            INSERT INTO audit_log (audit_id, method, path, user_id, status, audit_time) 
                VALUES (?, ?, ?, ?, ?, current_timestamp)
        """, request.attribute("audit_id"), request.requestMethod(), request.pathInfo(), request.attribute("subject"), response.status())
    }

    fun readAuditLog(request: Request, response: Response): JSONArray {
        fun recordToJson(row: ResultSet) = JSONObject().apply {
            put("id", row.getLong("audit_id"))
            put("method", row.getString("method"))
            put("path", row.getString("path"))
            put("status", row.getString("status"))
            put("time", row.getTimestamp("audit_time").toInstant())
        }

        val since = Instant.now().minus(1, ChronoUnit.HOURS)
        val logs = database.findAll(::recordToJson, "SELECT * FROM audit_log WHERE audit_time >= ? LIMIT 20", since)

        return JSONArray(logs)
    }
}