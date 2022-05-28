package xyz.haff.apisecurity.controller

import org.dalesbred.Database
import org.json.JSONObject
import spark.Request
import spark.Response

class SpaceController(val database: Database) {

    fun createSpace(request: Request, response: Response): JSONObject {
        val json = JSONObject(request.body())
        val spaceName = json.getString("name")
        val owner = json.getString("owner")

        return database.withTransaction {
            val spaceId = database.findUniqueLong("SELECT NEXT VALUE FOR space_id_seq")

            database.updateUnique(
                "INSERT INTO spaces(space_id, name, owner) VALUES (?, ?, ?)",
                spaceId,
                spaceName,
                owner
            )

            val spaceURI = "/spaces/$spaceId"
            response.status(201)
            response.header("Location", spaceURI)

            return@withTransaction JSONObject().apply {
                put("name", spaceName)
                put("uri", spaceURI)
            }
        }
    }
}