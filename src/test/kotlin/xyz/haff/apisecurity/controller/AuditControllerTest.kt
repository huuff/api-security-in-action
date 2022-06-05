package xyz.haff.apisecurity.controller

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import spark.Request
import spark.Response
import xyz.haff.apisecurity.database.AuditRepository
import java.time.Instant
import java.time.temporal.ChronoUnit

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

    test("audit response") {
        // ARRANGE
        val auditId = 1L
        val method = "GET"
        val path = "/test"
        val userId = "anon"
        val status = 201

        val request = mockk<Request> {
            every { attribute<Long>("audit_id") } returns auditId
            every { requestMethod() } returns method
            every { pathInfo() } returns path
            every { attribute<String>("subject") } returns userId
        }

        val response = mockk<Response> {
            every { status() } returns status
        }

        val savedAuditId = slot<Long>()
        val savedMethod = slot<String>()
        val savedPath = slot<String>()
        val savedUserId = slot<String>()
        val savedStatus = slot<Int>()

        val auditRepository = mockk<AuditRepository> {
            every {
                saveResponse(capture(savedAuditId), capture(savedMethod), capture(savedPath), capture(savedUserId), capture(savedStatus))
            } returns 2L
        }
        val auditController = AuditController(auditRepository)

        // ACT
        auditController.auditRequestEnd(request, response)

        // ASSERT
        savedAuditId.captured shouldBe auditId
        savedMethod.captured shouldBe method
        savedPath.captured shouldBe path
        savedUserId.captured shouldBe userId
        savedStatus.captured shouldBe status
    }

    test("read audit log") {
        // ARRANGE
        val now = Instant.now()
        val expectedSince = now.minus(1, ChronoUnit.HOURS)

        mockkStatic(Instant::class)
        every { Instant.now() } returns now

        val actualSince = slot<Instant>()
        val auditRepository = mockk<AuditRepository> {
            every { list(capture(actualSince)) } returns mockk()
        }
        val auditController = AuditController(auditRepository)

        // ACT
        auditController.readAuditLog(mockk(), mockk())

        // ASSERT
        actualSince.captured shouldBe expectedSince
    }
})