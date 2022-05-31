package xyz.haff.apisecurity.database

import org.dalesbred.Database

class SafeSpaceRepository(private val database: Database) : SpaceRepository {

    override fun save(name: String, owner: String): Long = database.run {
        withTransaction {
            val id = findUniqueLong("SELECT NEXT VALUE FOR space_id_seq")
            updateUnique("INSERT INTO spaces(space_id, name, owner) VALUES (?, ?, ?)", id, name, owner)
            id
        }
    }
}