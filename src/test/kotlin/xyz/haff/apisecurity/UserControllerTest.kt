package xyz.haff.apisecurity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import spark.HaltException
import spark.Request
import spark.Response
import spark.Spark
import xyz.haff.apisecurity.controller.UserController

class UserControllerTest : FunSpec({

    context("authentication") {
        test("returns 401 for missing authentication") {
            val userController = UserController(mockk(), mockk(), mockk())

            val request = mockk<Request> {
                every { attribute<String>("subject") } returns null
            }
            val response = mockk<Response>(relaxed = true)

            // I don't really now why, but halt throws this exception
            shouldThrow<HaltException> {
                userController.requireAuthentication(request, response)
            }
        }
    }
})