package xyz.haff.apisecurity.controller

import org.json.JSONArray
import spark.Request
import spark.Response
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.database.AuditRepository
import java.time.Instant
import java.time.temporal.ChronoUnit

// TODO: Test it
class AuditController(
    private val repository: AuditRepository,
) {

    fun auditRequestStart(request: Request, response: Response) {
        val auditId = repository.saveRequest(
            method = request.requestMethod(),
            path = request.pathInfo(),
            userId = request.attribute("subject")
        )
        request.attribute("audit_id", auditId)
    }

    fun auditRequestEnd(request: Request, response: Response): Unit {
        repository.saveResponse(
            auditId = request.attribute("audit_id"),
            method = request.requestMethod(),
            path = request.pathInfo(),
            userId = request.attribute("subject"),
            status = response.status(),
        )
    }

    fun readAuditLog(request: Request, response: Response): JSONArray {
        val since = Instant.now().minus(1, ChronoUnit.HOURS)
        return repository.list(since)
    }
}