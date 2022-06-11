package xyz.haff.apisecurity.database

import io.kotest.core.Tuple2
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.dalesbred.Database
import org.json.JSONObject
import xyz.haff.apisecurity.createDatabase
import java.time.Instant
import java.time.temporal.ChronoUnit

class AuditRepositoryTest : FunSpec({


    listOf<Tuple2<(Database) -> AuditRepository, String>>(
        Tuple2(::SafeAuditRepository, "safe audit repository"),
        Tuple2(::UnsafeAuditRepository, "unsafe audit repository"),
    ).forEach { (createRepository, repositoryName) ->
        test(repositoryName + ": correctly saves and lists request") {
            val repository = createRepository(createDatabase())
            val id = repository.saveRequest("GET", "/test", "someone")
            val saved = repository.list(Instant.now().minus(1, ChronoUnit.HOURS))

            println(saved)

            saved shouldHaveSize 1
            val element = saved[0] as JSONObject
            element["path"] shouldBe "/test"
            element["method"] shouldBe "GET"
            element["id"] shouldBe id
        }

        test(repositoryName + ": correctly saves and lists response") {
            val repository = createRepository(createDatabase())
            repository.saveResponse(1, "GET", "/test", "someone", 200)
            val saved = repository.list(Instant.now().minus(1, ChronoUnit.HOURS))

            println(saved)

            saved shouldHaveSize 1
            val element = saved[0] as JSONObject
            element["path"] shouldBe "/test"
            element["method"] shouldBe "GET"
            element["id"] shouldBe 1
            element["status"] shouldBe "200"
        }
    }
})