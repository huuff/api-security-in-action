package xyz.haff.apisecurity

import org.dalesbred.Database
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import xyz.haff.apisecurity.controller.AuditController
import xyz.haff.apisecurity.controller.SpaceController
import xyz.haff.apisecurity.controller.UserController
import xyz.haff.apisecurity.database.*

fun createInjectionContext(config: Config = Config.fromExternal()) = DI {
    bind<Config> { singleton { config } }
    bind<Database> { singleton { createDatabase(instance()) } }

    bind<SpaceRepository> {
        singleton {
            if (config.preparedStatements) { SafeSpaceRepository(instance()) }
            else { UnsafeSpaceRepository(instance()) }
        }
    }

    bind<UserRepository> {
        singleton {
            if (config.preparedStatements) { SafeUserRepository(instance()) }
            else { UnsafeUserRepository(instance()) }
        }
    }

    bind<AuditRepository> {
        singleton {
            if (config.preparedStatements) { SafeAuditRepository(instance()) }
            else { UnsafeAuditRepository(instance()) }
        }
    }

    bind<SpaceController> { singleton { SpaceController(instance(), instance()) } }
    bind<UserController> { singleton { UserController(instance(), instance()) } }
    bind<AuditController> { singleton { AuditController(instance(), instance()) } }
}