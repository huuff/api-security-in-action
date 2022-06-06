package xyz.haff.apisecurity

import org.kodein.di.instance
import org.kodein.di.newInstance

fun main(args: Array<String>) {
    val di = createInjectionContext()
    val serverConfigurer by di.newInstance { ServerConfigurer(instance(), instance(), instance(), instance()) }
    serverConfigurer.configure()
}
