package com.pucetec.events.controllers

import com.pucetec.events.dto.AttendeeRequest
import com.pucetec.events.dto.AttendeeResponse
import com.pucetec.events.services.AttendeeService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/attendees")
class AttendeeController(
    private val attendeeService: AttendeeService
) {
    private val logger = LoggerFactory.getLogger(AttendeeController::class.java)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAttendee(@RequestBody request: AttendeeRequest): AttendeeResponse {
        logger.info("Recibida petición para crear un asistente: ${request.name}")
        return attendeeService.createAttendee(request)
    }
}