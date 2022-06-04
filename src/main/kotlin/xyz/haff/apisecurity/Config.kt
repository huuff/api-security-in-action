package xyz.haff.apisecurity

import java.util.*

data class Config(
    val preparedStatements: Boolean = false,
    val dbUnprivilegedUser: Boolean = false,
    val inputValidation: Boolean = false,
    val jsonOnly: Boolean = false, // TODO: Test it
    val xssProtection: Boolean = false, // TODO: Test it
    val dontLeakServerInformation: Boolean = false,
    val contentSecurityPolicy: Boolean = false,
    val rateLimitPerSecond: Int = 0, // TODO: Test it
    val enableAuthentication: Boolean = false, // TODO: Test it
    val https: Boolean = false,
    val hsts: Boolean = false,
    val auditLogging: Boolean = false,
    val enableAuthorization: Boolean = false, // TODO: Test it
) {
    init {
        if (!https && hsts)
            throw IllegalStateException("Can't have HSTS without HTTPS!")

        if (!enableAuthentication && enableAuthorization)
            throw IllegalStateException("You have to enable authentication to enable authorization!")
    }

    companion object {
        // TODO: Implement superseding any of these from environment variables, it'll be useful when I make the switch
        // to kubernetes
        fun fromExternal(): Config {
            val properties = Properties().apply {
                load(Thread.currentThread().contextClassLoader.getResourceAsStream("config.properties"))
            }

            return Config(
                preparedStatements = (properties["PREPARED_STATEMENTS"] as String).toBooleanStrict(),
                dbUnprivilegedUser = (properties["DB_UNPRIVILEGED_USER"] as String).toBooleanStrict(),
                inputValidation = (properties["INPUT_VALIDATION"] as String).toBooleanStrict(),
                jsonOnly = (properties["JSON_ONLY"] as String).toBooleanStrict(),
                xssProtection = (properties["XSS_PROTECTION"] as String).toBooleanStrict(),
                dontLeakServerInformation = (properties["DONT_LEAK_SERVER_INFORMATION"] as String).toBooleanStrict(),
                contentSecurityPolicy = (properties["CONTENT_SECURITY_POLICY"] as String).toBooleanStrict(),
                rateLimitPerSecond = (properties["RATE_LIMIT_PER_SECOND"] as String).toInt(),
                enableAuthentication = (properties["ENABLE_AUTHENTICATION"] as String).toBooleanStrict(),
                auditLogging = (properties["AUDIT_LOGGING"] as String).toBooleanStrict(),
                enableAuthorization = (properties["ENABLE_AUTHORIZATION"] as String).toBooleanStrict(),
            )
        }
    }
}
