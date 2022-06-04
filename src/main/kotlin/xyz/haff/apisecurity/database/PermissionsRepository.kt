package xyz.haff.apisecurity.database

interface PermissionsRepository {

    fun save(spaceId: Long, userId: String, perms: String): Unit

    fun find(spaceId: Long, userId: String): String
}