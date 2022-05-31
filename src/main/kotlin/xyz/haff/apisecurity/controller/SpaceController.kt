package xyz.haff.apisecurity.controller

import org.json.JSONObject
import spark.Request
import spark.Response
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.database.SpaceRepository
import xyz.haff.apisecurity.util.NAME_PATTERN

// TODO: Properly separate concerns and clean code to accommodate varying configs? For example, the functionality
// might be separated into decorators.
class SpaceController(
    private val spaceRepository: SpaceRepository,
    private val config: Config,
) {

    // TODO: Implement post message (I didn't earlier?)
    fun createSpace(request: Request, response: Response): JSONObject {
        val json = JSONObject(request.body())
        val spaceName = json.getString("name")
        val owner = json.getString("owner")

        if (config.inputValidation) validateInput(spaceName, owner)

        if (config.enableAuthentication && (request.attribute<String>("subject") != owner)) {
            throw IllegalArgumentException("Owner must match authenticated user")
        }

        val spaceId = spaceRepository.save(spaceName, owner)

        val spaceURI = "/spaces/$spaceId"
        response.status(201)
        response.header("Location", spaceURI)

        return JSONObject().apply {
            put("name", spaceName)
            put("uri", spaceURI)
        }
    }

    private fun validateInput(spaceName: String, owner: String) {
        if (spaceName.length > 255) {
            throw IllegalArgumentException("space name must be less than 256 characters")
        }

        if (!owner.matches(NAME_PATTERN)) {
            throw IllegalArgumentException("usernames must start with a letter and contain only letters and numbers")
        }
    }
}