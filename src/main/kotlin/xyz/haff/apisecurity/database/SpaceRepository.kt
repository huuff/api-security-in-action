package xyz.haff.apisecurity.database

interface SpaceRepository {
    fun save(name: String, owner: String): Long
}