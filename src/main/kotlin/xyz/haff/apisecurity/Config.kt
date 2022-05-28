package xyz.haff.apisecurity

import java.util.*

data class Config(
    val preparedStatements: Boolean = false
) {
    companion object {
        fun fromProperties(): Config {
            val properties = Properties().apply {
                load(Thread.currentThread().contextClassLoader.getResourceAsStream("config.properties"))
            }

            return Config(
                preparedStatements = (properties["PREPARED_STATEMENTS"] as String).toBooleanStrict()
            )
        }
    }
}
