package xyz.haff.apisecurity.controller

import com.lambdaworks.crypto.SCryptUtil
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldNotThrowAny
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
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.database.PermissionsRepository
import xyz.haff.apisecurity.database.UserRepository
import java.util.*

class UserControllerTest : FunSpec({

    context("authentication") {

        test("can register user") {
            val expectedUsername = "anon"
            val savedUsername = slot<String>()
            val userRepository = mockk<UserRepository> {
                every { save(capture(savedUsername), any()) } returns Unit
            }
            val userController = UserController(userRepository, mockk(), Config(inputValidation = false))

            val request = mockk<Request> {
                every { body() } returns """{ "username": "$expectedUsername", "password": "anywoulddothankyou" }"""
            }

            val returnedStatus = slot<Int>()
            val returnedLocation = slot<String>()
            val response = mockk<Response> {
                every { status(capture(returnedStatus)) } returns Unit
                every { header("Location", capture(returnedLocation))} returns Unit
            }

            val jsonResponse = userController.registerUser(request, response)
            savedUsername.captured shouldBe expectedUsername
            returnedStatus.captured shouldBe 201
            returnedLocation.captured shouldBe "/users/$expectedUsername"
            jsonResponse.toString() shouldEqualJson """{ "username": "$expectedUsername" }"""

        }

        test("user gets authenticated") {
            // Arrange
            val username = "username"
            val password = "password"

            val hashedPassword = "fakeHashedPassword"
            val userRepository = mockk<UserRepository> {
                every { findHashedPassword(username) } returns hashedPassword
            }

            mockkStatic(SCryptUtil::class)
            every { SCryptUtil.check(password, hashedPassword) } returns true

            val actualSubject = slot<String>()
            val basicAuth = Base64.getEncoder().encode("$username:$password".toByteArray()).toString(Charsets.UTF_8)
            val request = mockk<Request> {
                every { headers("Authorization") } returns "Basic $basicAuth"
                every { attribute("subject", capture(actualSubject)) } returns Unit
            }

            val userController = UserController(userRepository, mockk(), Config(inputValidation = false))

            // Act
            userController.authenticate(request, mockk())

            // Assert
            actualSubject.captured shouldBe username
        }

        context("requiring authentication") {
            test("halts for missing authentication") {
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

            test("proceeds when authenticated") {
                val userController = UserController(mockk(), mockk(), mockk())

                val request = mockk<Request> {
                    every { attribute<String>("subject") } returns "anon"
                }
                val response = mockk<Response>(relaxed = true)

                shouldNotThrowAny {
                    userController.requireAuthentication(request, response)
                }
            }
        }
    }

    context("authorization") {
        test("halts when unauthorized") {
            val user = "anon"
            val userController = UserController(
                userRepository = mockk(),
                permissionsRepository = mockk<PermissionsRepository> {
                    every { find(any(), user) } returns ""
                },
                config = mockk(),
            )

            val filter = userController.requirePermission("GET", "r")

            shouldThrow<HaltException> {
                filter.handle(
                    mockk {
                        every { attribute<String>("subject") } returns user
                        every { params(":spaceId") } returns "1"
                        every { requestMethod() } returns "GET"
                    },
                    mockk(relaxed = true)
                )
            }
        }

        test("proceeds when authorized") {
            val user = "anon"
            val userController = UserController(
                userRepository = mockk(),
                permissionsRepository = mockk<PermissionsRepository> {
                    every { find(any(), user) } returns "rwd"
                },
                config = mockk(),
            )

            val filter = userController.requirePermission("GET", "r")

            shouldNotThrowAny {
                filter.handle(
                    mockk {
                        every { attribute<String>("subject") } returns user
                        every { params(":spaceId") } returns "1"
                        every { requestMethod() } returns "GET"
                    },
                    mockk(relaxed = true)
                )
            }
        }
    }
})