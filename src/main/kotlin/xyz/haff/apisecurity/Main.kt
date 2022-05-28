package xyz.haff.apisecurity

import org.dalesbred.Database
import org.h2.jdbcx.JdbcConnectionPool
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password")
    val database = Database.forDataSource(datasource)
}

fun createTables(database: Database) {
    val path = Paths.get(object {}.javaClass.getResource("/schema.sql")!!.toURI())
    database.update(Files.readString(path))
}
