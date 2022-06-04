package xyz.haff.apisecurity

import com.google.common.util.concurrent.RateLimiter
import org.dalesbred.result.EmptyResultException
import org.json.JSONException
import org.json.JSONObject
import org.kodein.di.instance
import org.kodein.di.newInstance
import spark.Request
import spark.Response
import spark.Spark.*
import xyz.haff.apisecurity.controller.AuditController
import xyz.haff.apisecurity.controller.SpaceController
import xyz.haff.apisecurity.controller.UserController
import xyz.haff.apisecurity.util.implies

class ServerConfigurer(
    private val config: Config,
    private val spaceController: SpaceController,
    private val userController: UserController,
    private val auditController: AuditController,
) {

    fun configure() {
        config.https implies { addHTTPS() }
        config.jsonOnly implies { addJsonHeaders() }
        (config.rateLimitPerSecond > 0) implies { addRateLimiting() }

        before(userController::authenticate)

        config.auditLogging implies { addAuditLogging() }

        config.enableAuthentication implies { before("/spaces", userController::requireAuthentication) }
        post("/spaces", spaceController::createSpace)
        post("/users", userController::registerUser)

        addExceptionHandlers()
        addResponseHeaders()
    }

    private fun addHTTPS() {
        val cert = object {}.javaClass.getResource("/certificate.p12")!!.file
        secure(cert, "changeit", null, null)
    }

    private fun addJsonHeaders() {
        before({ req, _ ->
            if (req.requestMethod() == "POST" && req.contentType() != "application/json") {
                halt(415, JSONObject().apply { put("error", "Only application/json supported") }.toString())
            }
        })
        after({ _, res -> res.type("application/json") })
    }

    private fun addRateLimiting() {
        val rateLimiter = RateLimiter.create(config.rateLimitPerSecond.toDouble())
        before({ _, res ->
            if (!rateLimiter.tryAcquire()) {
                res.header("Retry-After", "2")
                halt(429)
            }
        })
    }

    private fun addAuditLogging() {
            before(auditController::auditRequestStart)
            afterAfter(auditController::auditRequestEnd)
            get("/logs", auditController::readAuditLog)
    }

    private fun addExceptionHandlers() {
        fun badRequest(ex: Exception, request: Request, response: Response) {
            response.status(400)
            response.body(JSONObject().apply { put("error", ex.message) }.toString())
        }

        internalServerError(JSONObject().apply {
            put("error", "internal server error")
        }.toString())

        notFound(JSONObject().apply {
            put("error", "not found")
        }.toString())

        exception(IllegalArgumentException::class.java, ::badRequest)
        exception(JSONException::class.java, ::badRequest)
        exception(EmptyResultException::class.java) { _, _, res -> res.status(400) }
    }

    private fun addResponseHeaders() {
        afterAfter { _, res ->
            with(res) {
                if (config.jsonOnly) {
                    type("application/json;charset=utf-8")
                }

                if (config.dontLeakServerInformation) {
                    header("Server", "")
                }

                // Disable cache
                header("Cache-Control", "no-store")

                if (config.contentSecurityPolicy) {
                    // default-src: 'none': prevent the response from loading scripts or resources
                    // frame-ancestors: 'none': prevent the response from being loaded into an iframe
                    // sandbox: disables scripts from being executed
                    header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox")
                }

                if (config.xssProtection) {
                    // Don't let the browser incorrectly guess the content type
                    header("X-Content-Type-Options", "nosniff")
                    // Ironically, xssProtection disables X-XSS-Protection, since that has vulnerabilities of its own
                    header("X-XSS-Protection", "0")
                    // Prevent responses from being loaded in an iframe
                    header("X-Frame-Options", "DENY")
                }

                if (config.hsts) {
                    header("Strict-Transport-Security", "max-age=31536000")
                }
            }
        }
    }
}