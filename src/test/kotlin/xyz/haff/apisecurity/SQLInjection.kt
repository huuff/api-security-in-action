package xyz.haff.apisecurity

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.dalesbred.DatabaseSQLException
import spark.Request
import spark.Response
import xyz.haff.apisecurity.controller.SpaceController

class SQLInjection : FunSpec({

    test("can inject SQL when there are no protections") {
        val config = Config(preparedStatements = false)
        val database = createDatabase()
        val spaceController = SpaceController(database, config)

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
        val config = Config(preparedStatements = true)
        val database = createDatabase()
        val spaceController = SpaceController(database, config)

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
        val config = Config(dbUnprivilegedUser = true)
        val database = createDatabase(config)
        val spaceController = SpaceController(database, config)

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

})