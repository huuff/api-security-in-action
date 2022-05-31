package xyz.haff.apisecurity

import org.dalesbred.Database
import org.kodein.di.*
import xyz.haff.apisecurity.controller.AuditController
import xyz.haff.apisecurity.controller.SpaceController
import xyz.haff.apisecurity.controller.UserController

// TODO: I'm sure I can test each controller independently, at the bare minimum
fun main(args: Array<String>) {
    val di = DI {
        bind<Config> { singleton { Config.fromExternal() }}
        bind<Database> { singleton { createDatabase(instance()) }}
        bind<SpaceController> { singleton { SpaceController(instance(), instance()) }}
        bind<UserController> { singleton { UserController(instance(), instance() )  }}
        bind<AuditController> { singleton { AuditController(instance(), instance()) }}
    }

    val serverConfigurer by di.newInstance { ServerConfigurer(instance(), instance(), instance(), instance()) }
    serverConfigurer.configure()
}


