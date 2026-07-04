package com.pucetec.events.mappers

import com.pucetec.events.dto.ReservationResponse
import com.pucetec.events.entities.Reservation

fun Reservation.toResponse() = ReservationResponse(
    id = this.id,
    status = this.status,
    createdAt = this.createdAt,
    event = this.event.toResponse(), // Traduce la entidad Event interna a DTO
    attendee = this.attendee.toResponse() // Lo mismo de arriba
)