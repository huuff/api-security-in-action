package xyz.haff.apisecurity.database

import org.dalesbred.Database
import org.json.JSONArray
import java.time.Instant

class SafeAuditRepository(private val database: Database) : AuditRepository() {
    // TODO: Remember to wrap these with some method that takes ID from a sequence
    override fun saveRequest(method: String, path: String, userId: String?): Long = with(database) {
        withTransaction {
            val id = findUniqueLong("SELECT NEXT VALUE FOR audit_id_seq")
            updateUnique(
                """
            INSERT INTO audit_log(audit_id, method, path, user_id, audit_time)
                VALUES (?, ?, ?, ?, current_timestamp)
        """, id, method, path, userId
            )
            id
        }
    }

    override fun saveResponse(auditId: Long, method: String, path: String, userId: String?, status: Int): Long =
        with(database) {
            updateUnique("""
            INSERT INTO audit_log (audit_id, method, path, user_id, status, audit_time) 
                VALUES (?, ?, ?, ?, ?, current_timestamp)
        """, auditId, method, path, userId, status)
            auditId
        }

    override fun list(since: Instant): JSONArray = JSONArray(
        database.findAll(::rowToJson, "SELECT * FROM audit_log WHERE audit_time >= ? LIMIT 20", since)
    )
}