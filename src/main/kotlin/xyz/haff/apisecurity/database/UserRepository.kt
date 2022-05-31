package xyz.haff.apisecurity.database

interface UserRepository {

    fun save(userId: String, hashedPassword: String): Unit
    fun findHashedPassword(userId: String): String?
}