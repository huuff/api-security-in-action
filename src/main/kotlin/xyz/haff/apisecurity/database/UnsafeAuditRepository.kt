package xyz.haff.apisecurity.database

import org.dalesbred.Database
import org.json.JSONArray
import java.sql.Timestamp
import java.time.Instant

class UnsafeAuditRepository(private val database: Database): AuditRepository() {
    override fun saveRequest(method: String, path: String, userId: String?): Long = with(database) {
        withTransaction {
            val id = findUniqueLong("SELECT NEXT VALUE FOR audit_id_seq")
            updateUnique("""
            INSERT INTO audit_log(audit_id, method, path, user_id, audit_time)
                VALUES ($id, '$method', '$path', '$userId', current_timestamp)
        """)
            id
        }
    }

    override fun saveResponse(auditId: Long, method: String, path: String, userId: String?, status: Int): Long =
    with(database) {
        updateUnique("""
            INSERT INTO audit_log (audit_id, method, path, user_id, status, audit_time) 
                VALUES ($auditId, '$method', '$path', '$userId', $status, current_timestamp)
        """)
        auditId
    }

    override fun list(since: Instant): JSONArray = JSONArray(
        database.findAll(::rowToJson, "SELECT * FROM audit_log WHERE audit_time >= '${Timestamp.from(since)}' LIMIT 20")
    )
}