package xyz.haff.apisecurity

import java.util.*

data class Config(
    val preparedStatements: Boolean = false,
    val dbUnprivilegedUser: Boolean = false,
    val inputValidation: Boolean = false,
    val jsonOnly: Boolean = false, // TODO: Test it
    val xssProtection: Boolean = false, // TODO: Test it
    val dontLeakServerInformation: Boolean = false,
) {
    companion object {
        fun fromProperties(): Config {
            val properties = Properties().apply {
                load(Thread.currentThread().contextClassLoader.getResourceAsStream("config.properties"))
            }

            return Config(
                preparedStatements = (properties["PREPARED_STATEMENTS"] as String).toBooleanStrict(),
                dbUnprivilegedUser = (properties["DB_UNPRIVILEGED_USER"] as String).toBooleanStrict(),
                inputValidation = (properties["INPUT_VALIDATION"] as String).toBooleanStrict(),
                jsonOnly = (properties["JSON_ONLY"] as String).toBooleanStrict(),
                xssProtection = (properties["XSS_PROTECTION"] as String).toBooleanStrict(),
                dontLeakServerInformation = (properties["DONT_LEAK_SERVER_INFORMATION"] as String).toBooleanStrict()
            )
        }
    }
}
