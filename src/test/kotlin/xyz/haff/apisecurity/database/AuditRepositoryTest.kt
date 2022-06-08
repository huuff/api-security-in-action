package xyz.haff.apisecurity.database

import io.kotest.core.Tuple2
import io.kotest.core.spec.style.FunSpec
import org.dalesbred.Database
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.createDatabase

class AuditRepositoryTest: FunSpec({

    listOf<() -> Tuple2<AuditRepository, Database>>(
        { createDatabase(Config()).let { Tuple2(SafeAuditRepository(it), it) } },
        { createDatabase(Config()).let { Tuple2(SafeAuditRepository(it), it) } },
    ).forEach {
        val (repository, database) = it()
        test("correctly saves and lists " + repository.javaClass.name) {

        }
    }
})