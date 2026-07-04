package com.pucetec.events.controllers

import com.pucetec.events.dto.EventRequest
import com.pucetec.events.dto.EventResponse
import com.pucetec.events.entities.Event
import com.pucetec.events.services.EventService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventController (
    private val eventService: EventService,
){
    private val logger = LoggerFactory.getLogger(EventController::class.java)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent(@RequestBody request: EventRequest): EventResponse {
        logger.info("Recibida petición para crear un evento: ${request.name}")
        return eventService.createEvent(request)
    }

    @GetMapping
    fun getAllEvents(): List<EventResponse> {
        logger.info("Peticion recibida para listar todos los eventos")
        return eventService.getAllEvents()
    }

    @GetMapping("/{id}")
    fun getEventById(@PathVariable id: Long): EventResponse {
        logger.info("Recibida petición para obtener el evento con ID: $id")
        return eventService.getEventById(id)
    }
}