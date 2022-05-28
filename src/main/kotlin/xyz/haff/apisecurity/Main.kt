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

    afterAfter { _, res ->
        if (config.jsonOnly) {
            res.type("application/json;charset=utf-8")
        }
        res.header("Server", "")
    }
}

private fun badRequest(ex: Exception, request: Request, response: Response) {
    response.status(400)
    response.body(JSONObject().apply { put("error", ex.message) }.toString())
}
