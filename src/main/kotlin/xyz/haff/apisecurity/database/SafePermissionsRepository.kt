package xyz.haff.apisecurity.database

import org.dalesbred.Database
import org.dalesbred.integration.kotlin.findOptional

class SafePermissionsRepository(private val database: Database) : PermissionsRepository {

    override fun save(spaceId: Long, userId: String, perms: String) {
        database.updateUnique("""
            INSERT INTO permissions(space_id, user_id, perms) VALUES (?, ?, ?)
        """, spaceId, userId, perms)
    }

    override fun find(spaceId: Long, userId: String): String = database.findOptional<String>("""
        SELECT perms FROM permissions WHERE space_id = ? AND user_id = ?
    """, spaceId, userId).orElse("")
}