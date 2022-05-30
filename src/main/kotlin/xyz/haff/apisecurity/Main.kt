package xyz.haff.apisecurity

import org.dalesbred.result.EmptyResultException
import org.json.JSONException
import org.json.JSONObject
import spark.Request
import spark.Response
import spark.Spark.*
import xyz.haff.apisecurity.controller.SpaceController

fun main(args: Array<String>) {
    val config = Config.fromProperties()
    val database = createDatabase(config)

    val spaceController = SpaceController(database, config)

    if (config.jsonOnly) {
        before({req, res ->
            if (req.requestMethod() == "POST" && req.contentType() != "application/json") {
                halt(415, JSONObject().apply { put("error", "Only application/json supported")}.toString())
            }
        })
    }

    post("/spaces", spaceController::createSpace)

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
        // Don't leak server information
        header("Server", "")

        // Disable cache
        header("Cache-Control", "no-store")

        // default-src: 'none': prevent the response from loading scripts or resources
        // frame-ancestors: 'none': prevent the response from being loaded into an iframe
        // sandbox: disables scripts from being executed
        header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox")
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
