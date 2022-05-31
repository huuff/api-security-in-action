package xyz.haff.apisecurity.database

import org.dalesbred.Database
import org.dalesbred.integration.kotlin.findOptional

class SafeUserRepository(private val database: Database): UserRepository {

    override fun save(userId: String, hashedPassword: String) = database.updateUnique("""
        INSERT INTO users(user_id, pw_hash) VALUES(?, ?)
    """.trimIndent(), userId, hashedPassword)

    override fun findHashedPassword(userId: String): String? = database.findOptional<String>("""
        SELECT pw_hash FROM users WHERE user_id = ?
    """.trimIndent(), userId).orElse(null)
}