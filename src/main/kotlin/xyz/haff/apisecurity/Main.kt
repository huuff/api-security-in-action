package xyz.haff.apisecurity

import org.dalesbred.Database
import org.h2.jdbcx.JdbcConnectionPool
import org.json.JSONObject
import spark.Spark.*
import xyz.haff.apisecurity.controller.SpaceController
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password")
    val database = Database.forDataSource(datasource)
    createTables(database)

    val spaceController = SpaceController(database)

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

fun createTables(database: Database) {
    val path = Paths.get(object {}.javaClass.getResource("/schema.sql")!!.toURI())
    database.update(Files.readString(path))
}
