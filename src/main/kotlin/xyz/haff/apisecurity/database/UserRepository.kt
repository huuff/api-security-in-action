package xyz.haff.apisecurity.database

// TODO: I might be able to have a generic CRUDRepository, when I implement all functionality (in space controller for example)
interface UserRepository {

    fun save(userId: String, hashedPassword: String): Unit
    fun findHashedPassword(userId: String): String?
}