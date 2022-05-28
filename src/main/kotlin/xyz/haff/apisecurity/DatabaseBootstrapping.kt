package xyz.haff.apisecurity

import org.dalesbred.Database
import org.h2.jdbcx.JdbcConnectionPool
import java.nio.file.Files
import java.nio.file.Paths

fun createDatabase(config: Config = Config()): Database {
    val adminDatasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password")
    val adminDatabase = Database.forDataSource(adminDatasource)
    adminDatabase.update("DROP ALL OBJECTS")

    val schema = Paths.get(object {}.javaClass.getResource("/schema.sql")!!.toURI())
    adminDatabase.update(Files.readString(schema))

    return if (config.dbUnprivilegedUser) {
        val unprivilegedDatasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter_api_user", "password")
        Database.forDataSource(unprivilegedDatasource)
    } else {
        adminDatabase
    }
}