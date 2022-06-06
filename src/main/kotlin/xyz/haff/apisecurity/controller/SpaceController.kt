package xyz.haff.apisecurity.controller

import org.json.JSONObject
import spark.Request
import spark.Response
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.database.PermissionsRepository
import xyz.haff.apisecurity.database.SpaceRepository
import xyz.haff.apisecurity.util.NAME_PATTERN
import xyz.haff.apisecurity.util.implies

// TODO: Properly separate concerns and clean code to accommodate varying configs? For example, the functionality
// might be separated into decorators.
class SpaceController(
    private val spaceRepository: SpaceRepository,
    private val permissionsRepository: PermissionsRepository,
    private val config: Config,
) {

    // TODO: Implement post message (I didn't earlier?)
    fun createSpace(request: Request, response: Response): JSONObject {
        val json = JSONObject(request.body())
        val spaceName = json.getString("name")
        val owner = json.getString("owner")

        config.inputValidation implies { validateInput(spaceName, owner) }

        if (config.enableAuthentication && (request.attribute<String>("subject") != owner)) {
            throw IllegalArgumentException("Owner must match authenticated user")
        }

        val spaceId = spaceRepository.save(spaceName, owner)

        config.enableAuthorization implies {
            permissionsRepository.save(spaceId, owner, "rwd")
        }

        val spaceURI = "/spaces/$spaceId"
        response.status(201)
        response.header("Location", spaceURI)

        return JSONObject().apply {
            put("name", spaceName)
            put("uri", spaceURI)
        }
    }

    fun addMember(request: Request, response: Response): JSONObject {
        val json = JSONObject(request.body())
        val spaceId = request.params(":spaceId").toLong()
        val userToAdd = json.getString("username")
        val permissions = json.getString("permissions")

        if (!permissions.matches(Regex.fromLiteral("r?w?d?"))) {
            throw IllegalArgumentException("Invalid permissions")
        }

        permissionsRepository.save(spaceId, userToAdd, permissions)

        response.status(200)
        return JSONObject().apply {
            put("username", userToAdd)
            put("permissions", permissions)
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