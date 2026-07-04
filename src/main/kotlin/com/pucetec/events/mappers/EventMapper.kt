package com.pucetec.events.mappers

import com.pucetec.events.dto.EventRequest
import com.pucetec.events.dto.EventResponse
import com.pucetec.events.entities.Event

/***
 * Convierte un EventRequest (lo que envia el cliente) a una Entity Event (lo que entiende la base de datos)
 * 'totalTickets' se usa tambnien para inicializar 'availableTickets' segun las reglas del negocio
 */
fun EventRequest.toEntity() = Event(
    name = this.name,
    venue = this.venue,
    totalTickets = this.totalTickets
)

/**
 * Convierte una Entity Event de la base de datos a un EventResponse para el cliente.
 */
fun Event.toResponse() = EventResponse(
    id = this.id,
    name = this.name,
    venue = this.venue,
    totalTickets = this.totalTickets,
    availableTickets = this.availableTickets

)

