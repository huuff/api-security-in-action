package xyz.haff.apisecurity.controller

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.Tuple3
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import spark.Request
import spark.Response
import xyz.haff.apisecurity.Config
import xyz.haff.apisecurity.database.SpaceRepository

class SpaceControllerTest : FunSpec({

    fun buildRequest(owner: String, spaceName: String) = """
                {
                    "name": "$spaceName",
                    "owner": "$owner",
                }
            """

    test("gives a correct response") {
        // ARRANGE
        val (spaceId, owner, spaceName) = Tuple3(1L, "owner", "space")
        val mockSpaceRepository = mockk<SpaceRepository> { every { save(any(), any()) } returns spaceId }

        val spaceController = SpaceController(
            spaceRepository = mockSpaceRepository,
            permissionsRepository = mockk(relaxed = true),
            config = Config(),
        )

        val request = mockk<Request> { every { body() } returns buildRequest(owner, spaceName) }
        val response = mockk<Response>(relaxed = true)

        // ACT
        val responseBody = spaceController.createSpace(request, response)

        // ASSERT
        verify { response.status(201) }
        verify { response.header("Location", "/spaces/$spaceId") }

        responseBody.toString() shouldEqualJson """
            {
                "name": "$spaceName",
                "uri": "/spaces/$spaceId"
            }
        """.trimIndent()
    }

    test("a space is actually created") {

    }

    test("an owner is given full privileges") {

    }
})