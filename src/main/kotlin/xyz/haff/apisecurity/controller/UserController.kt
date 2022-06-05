package xyz.haff.apisecurity.controller

import com.lambdaworks.crypto.SCryptUtil
import org.json.JSONObject
import spark.Filter
import spark.Request
import spark.Response
import spark.Spark.halt
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.database.PermissionsRepository
import xyz.haff.apisecurity.database.UserRepository
import xyz.haff.apisecurity.util.NAME_PATTERN
import java.nio.charset.StandardCharsets
import java.util.*

class UserController(
    private val userRepository: UserRepository,
    private val permissionsRepository: PermissionsRepository,
    private val config: Config,
) {
    fun registerUser(request: Request, response: Response): JSONObject {
        val json = JSONObject(request.body())
        val username = json.getString("username")
        val password = json.getString("password")

        if (config.inputValidation) {
            if (!username.matches(NAME_PATTERN))
                throw IllegalArgumentException("Invalid username")
            if (password.length < 8)
                throw IllegalArgumentException("Password must be at least eight characters")
        }

        val hash = SCryptUtil.scrypt(password, 32768, 8, 1)
        userRepository.save(username, hash)

        response.status(201)
        response.header("Location", "/users/$username")
        return JSONObject().apply { put("username", username) }
    }

    fun authenticate(request: Request, response: Response) {
        val authHeader = request.headers("Authorization")
        if (authHeader == null || !authHeader.startsWith("Basic "))
            return;

        val authPayload = authHeader.replace("Basic ", "")
        val credentials = String(Base64.getDecoder().decode(authPayload), StandardCharsets.UTF_8)
        val (username, password) = credentials.split(":")

        if (config.inputValidation && !username.matches(NAME_PATTERN))
            throw IllegalArgumentException("Invalid username")

        val hash = userRepository.findHashedPassword(username)
        if (hash?.let { SCryptUtil.check(password, hash)} == true) {
            request.attribute("subject", username)
        }
    }

    fun requireAuthentication(request: Request, response: Response) {
        if (request.attribute<String>("subject") == null) {
            response.header("WWW-Authenticate", """Basic realm="", charset="UTF-8"""")
            halt(401)
        }
    }

    // TODO: Test it
    fun requirePermission(method: String, requiredPermissions: String): Filter = Filter { request: Request, response: Response ->
        if (request.requestMethod() != method) {
            return@Filter
        }

        requireAuthentication(request, response)

        val spaceId = request.params(":spaceId").toLong()
        val username = request.attribute<String>("subject")

        val permissions = permissionsRepository.find(spaceId, username)

        if (requiredPermissions !in permissions)
            halt(403)
    }
}