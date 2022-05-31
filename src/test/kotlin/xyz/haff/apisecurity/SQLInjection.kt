package xyz.haff.apisecurity

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.dalesbred.Database
import org.dalesbred.DatabaseSQLException
import org.kodein.di.instance
import org.kodein.di.newInstance
import spark.Request
import spark.Response
import xyz.haff.apisecurity.controller.SpaceController

class SQLInjection : FunSpec({

    data class SpaceControllerAndDatabase(val spaceController: SpaceController, val database: Database)
    fun getDeps(config: Config) = createInjectionContext(config).run {
        val controller by instance<SpaceController>()
        val database by instance<Database>()
        SpaceControllerAndDatabase(controller, database)
    }

    test("can inject SQL when there are no protections") {
        val (spaceController, database ) = getDeps(Config(preparedStatements = false))

        val request = mockk<Request> {
            every { body() } returns """
                {
                    "name": "test",
                    "owner": "'); DROP TABLE spaces; --"
                }
            """.trimIndent()
        }

        val response = mockk<Response>(relaxed = true)
        spaceController.createSpace(request, response)

        verify { response.status(201) }
        val exception = shouldThrow<DatabaseSQLException> { database.findTable("SELECT * FROM spaces") }
        exception.message shouldContain """Table "SPACES" not found"""
    }

    test("can't inject SQL with prepared statements") {
        val (spaceController, database) = getDeps(Config(preparedStatements = true))

        val request = mockk<Request> {
            every { body() } returns """
                {
                    "name": "'); DROP TABLE spaces; --",
                    "owner": ""
                }
            """.trimIndent()
        }

        val response = mockk<Response>(relaxed = true)
        val responseBody = spaceController.createSpace(request, response)

        verify { response.status(201) }
        shouldNotThrowAny { database.findTable("SELECT * FROM spaces") }
        responseBody["name"] shouldBe "'); DROP TABLE spaces; --"
    }

    test("can't inject SQL with unprivileged user") {
        val (spaceController, _) = getDeps(Config(dbUnprivilegedUser = true))

        val request = mockk<Request> {
            every { body() } returns """
                {
                    "name": "test",
                    "owner": "'); DROP TABLE spaces; --"
                }
            """.trimIndent()
        }
        val response = mockk<Response>(relaxed = true)

        val exception = shouldThrow<DatabaseSQLException> { spaceController.createSpace(request, response) }
        exception.message shouldContain "Not enough rights"
    }

    // Actually, it's still perfectly possible to inject SQL through space name, since that doesn't get validated for
    // symbols
    test("can't inject SQL with input validation") {
        val (spaceController, _) = getDeps(Config(inputValidation = true))

        val request = mockk<Request> {
            every { body() } returns """
                {
                    "name": "test",
                    "owner": "'); DROP TABLE spaces; --"
                }
            """.trimIndent()
        }
        val response = mockk<Response>(relaxed = true)

        shouldThrow<IllegalArgumentException> { spaceController.createSpace(request, response) }
    }

})