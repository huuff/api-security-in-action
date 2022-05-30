package xyz.haff.apisecurity

import com.google.common.util.concurrent.RateLimiter
import org.dalesbred.result.EmptyResultException
import org.json.JSONException
import org.json.JSONObject
import spark.Request
import spark.Response
import spark.Spark.*
import xyz.haff.apisecurity.controller.SpaceController
import xyz.haff.apisecurity.controller.UserController

fun main(args: Array<String>) {
    val config = Config.fromProperties()
    val database = createDatabase(config)

    val spaceController = SpaceController(database, config)
    val userController = UserController(database, config)

    if (config.jsonOnly) {
        before({req, _ ->
            if (req.requestMethod() == "POST" && req.contentType() != "application/json") {
                halt(415, JSONObject().apply { put("error", "Only application/json supported")}.toString())
            }
        })
    }

    if (config.rateLimitPerSecond > 0) {
        val rateLimiter = RateLimiter.create(config.rateLimitPerSecond.toDouble())
        before({ _, res ->
            if (!rateLimiter.tryAcquire()) {
                res.header("Retry-After", "2")
                halt(429)
            }
        })
    }

    before(userController::authenticate)
    post("/spaces", spaceController::createSpace)
    post("/users", userController::registerUser)

    after({ _, response ->
        response.type("application/json")
    })

    internalServerError(JSONObject().apply {
        put("error", "internal server error")
    }.toString())

    notFound(JSONObject().apply {
        put("error", "not found")
    }.toString())

    exception(IllegalArgumentException::class.java, ::badRequest)
    exception(JSONException::class.java, ::badRequest)
    exception(EmptyResultException::class.java) { _, _, res -> res.status(400) }

    afterAfter { _, res -> with(res) {
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
        }}
    }
}

private fun badRequest(ex: Exception, request: Request, response: Response) {
    response.status(400)
    response.body(JSONObject().apply { put("error", ex.message) }.toString())
}
