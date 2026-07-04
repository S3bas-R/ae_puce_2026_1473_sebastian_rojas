package com.pucetec.events.dto

import java.time.LocalDateTime

/**
 * Lo que el cliente envía para reservar una entrada.
 * Solo necesita decirnos QUIÉN (attendeeId) va a CUÁL evento (eventId).
 */
data class ReservationRequest (
    var attendeeId: Long,
    val eventId: Long
)

/**
 * Lo que devolvemos cuando se consulta una reserva.
 * En lugar de mapear toda la entidad compleja, enviamos datos planos o mini-respuestas.
 */
data class ReservationResponse (
    val id: Long,
    val status: String,
    val createdAt: LocalDateTime,
    val event: EventResponse,
    val attendee: AttendeeResponse
)
