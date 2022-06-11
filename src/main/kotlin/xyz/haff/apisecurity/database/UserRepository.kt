package xyz.haff.apisecurity.database

interface UserRepository {

    fun save(userId: String, hashedPassword: String)
    fun findHashedPassword(userId: String): String?
}