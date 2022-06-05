package xyz.haff.apisecurity.controller

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import spark.Request
import xyz.haff.apisecurity.database.AuditRepository

class AuditControllerTest : FunSpec({

    test("audit request") {
        // ARRANGE
        val method = "GET"
        val path = "/test"
        val userId = "userId"
        val auditId = 1L

        val savedMethod = slot<String>()
        val savedPath = slot<String>()
        val savedUserId = slot<String>()
        val auditRepository = mockk<AuditRepository> {
            every { saveRequest(capture(savedMethod), capture(savedPath), capture(savedUserId)) } returns auditId
        }

        val auditController = AuditController(auditRepository)
        val auditIdSetInRequest = slot<Long>()
        val request = mockk<Request> {
            every { requestMethod() } returns method
            every { pathInfo() } returns path
            every { attribute<String>("subject") } returns userId
            every { attribute("audit_id", capture(auditIdSetInRequest)) } returns Unit
        }

        // ACT
        auditController.auditRequestStart(request, mockk())

        // ASSERT
        savedMethod.captured shouldBe method
        savedPath.captured shouldBe path
        savedUserId.captured shouldBe userId
        auditIdSetInRequest.captured shouldBe auditId
    }
})