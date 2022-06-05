package xyz.haff.apisecurity.controller

import com.lambdaworks.crypto.SCryptUtil
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.Tuple3
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
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
            val username = "anon"
            val userRepository = mockk<UserRepository>(relaxed = true)
            val userController = UserController(userRepository, mockk(), Config(inputValidation = false))

            val request = mockk<Request> {
                every { body() } returns """{ "username": "$username", "password": "anywoulddothankyou" }"""
            }
            val response = mockk<Response>(relaxed = true)

            // ACT
            val jsonResponse = userController.registerUser(request, response)

            // ASSERT
            verify {
                userRepository.save(userId = username, hashedPassword = any())
                response.status(201)
                response.header("Location", "/users/$username")
            }

            jsonResponse.toString() shouldEqualJson """{ "username": "$username" }"""
        }

        test("user gets authenticated") {
            // Arrange
            val (username, password, hashedPassword) = Tuple3("username", "password", "fakeHashedPassword")

            val userRepository = mockk<UserRepository> {
                every { findHashedPassword(username) } returns hashedPassword
            }

            mockkStatic(SCryptUtil::class)
            every { SCryptUtil.check(password, hashedPassword) } returns true

            val basicAuth = Base64.getEncoder().encode("$username:$password".toByteArray()).toString(Charsets.UTF_8)
            val request = mockk<Request>(relaxed = true) {
                every { headers("Authorization") } returns "Basic $basicAuth"
            }

            val userController = UserController(userRepository, mockk(), Config(inputValidation = false))

            // Act
            userController.authenticate(request, mockk())

            // Assert
            verify { request.attribute("subject", username) }
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