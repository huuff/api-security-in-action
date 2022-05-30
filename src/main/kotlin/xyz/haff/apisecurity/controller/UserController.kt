package xyz.haff.apisecurity.controller

import com.lambdaworks.crypto.SCryptUtil
import org.dalesbred.Database
import org.json.JSONObject
import spark.Request
import spark.Response
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.util.NAME_PATTERN

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
}