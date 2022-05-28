package xyz.haff.apisecurity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
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

})