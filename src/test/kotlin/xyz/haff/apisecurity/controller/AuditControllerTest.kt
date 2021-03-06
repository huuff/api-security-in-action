package xyz.haff.apisecurity.controller

import io.kotest.core.Tuple4
import io.kotest.core.Tuple5
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import spark.Request
import spark.Response
import xyz.haff.apisecurity.database.AuditRepository
import java.time.Instant
import java.time.temporal.ChronoUnit

class AuditControllerTest : FunSpec({

    test("audit request") {
        // ARRANGE
        val (method, path, userId, auditId) = Tuple4("GET", "/test", "user", 1L)
        val request = mockk<Request>(relaxed = true) {
            every { requestMethod() } returns method
            every { pathInfo() } returns path
            every { attribute<String>("subject") } returns userId
        }

        val auditRepository = mockk<AuditRepository> {
            every { saveRequest(any(), any(), any()) } returns auditId
        }

        // ACT
        AuditController(auditRepository).auditRequestStart(request, mockk())

        // ASSERT
        verify {
            request.attribute("audit_id", auditId)
            auditRepository.saveRequest(method, path, userId)
        }
    }

    test("audit response") {
        // ARRANGE
        val (auditId, method, path, userId, status) = Tuple5(1L, "GET", "/test", "anon", 201)

        val request = mockk<Request> {
            every { attribute<Long>("audit_id") } returns auditId
            every { requestMethod() } returns method
            every { pathInfo() } returns path
            every { attribute<String>("subject") } returns userId
        }

        val response = mockk<Response> {
            every { status() } returns status
        }

        val auditRepository = mockk<AuditRepository> {
            every { saveResponse(any(), any(), any(), any(), any()) } returns 2L
        }
        val auditController = AuditController(auditRepository)

        // ACT
        auditController.auditRequestEnd(request, response)

        // ASSERT
        verify { auditRepository.saveResponse(auditId, method, path, userId, status) }
    }

    test("read audit log") {
        // ARRANGE
        val now = Instant.now()
        val expectedSince = now.minus(1, ChronoUnit.HOURS)

        mockkStatic(Instant::class)
        every { Instant.now() } returns now

        val auditRepository = mockk<AuditRepository> {
            every { list(any()) } returns mockk()
        }
        val auditController = AuditController(auditRepository)

        // ACT
        auditController.readAuditLog(mockk(), mockk())

        // ASSERT
        verify { auditRepository.list(expectedSince) }
    }
})