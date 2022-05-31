package xyz.haff.apisecurity.controller

import com.lambdaworks.crypto.SCryptUtil
import org.dalesbred.Database
import org.dalesbred.integration.kotlin.findOptional
import org.json.JSONObject
import spark.Request
import spark.Response
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.util.NAME_PATTERN
import java.nio.charset.StandardCharsets
import java.util.*

// TODO: Repositories for safe/unsafe
class UserController(
    private val database: Database,
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
        database.updateUnique("INSERT INTO users(user_id, pw_hash) VALUES(?, ?)", username, hash)

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

        val hash = database.findOptional<String>("SELECT pw_hash FROM users WHERE user_id = ?", username)
        if (hash.isPresent && SCryptUtil.check(password, hash.get())) {
            request.attribute("subject", username)
        }

    }
}