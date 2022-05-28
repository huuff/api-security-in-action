package xyz.haff.apisecurity

import org.json.JSONObject
import spark.Spark.*
import xyz.haff.apisecurity.controller.SpaceController

fun main(args: Array<String>) {
    val database = createDatabase()
    val config = Config.fromProperties()

    val spaceController = SpaceController(database, config)

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
}
