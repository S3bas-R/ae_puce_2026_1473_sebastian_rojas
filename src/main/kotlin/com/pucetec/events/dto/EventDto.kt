package com.pucetec.events.dto
/**
 * Lo que el cliente nos envía en el cuerpo (JSON) al crear un evento.
 * No incluye 'id' ni 'availableTickets' porque el sistema los genera automáticamente.
 */
data class EventRequest(
    val name: String,
    val venue: String,
    val totalTickets: Int
)

/**
 * Lo que le respondemos al cliente cuando consulta un evento.
 * Aquí mostramos all, incluyendo los tickets disponibles calculados.
 */
data class EventResponse(
    val id: Long,
    val name: String,
    val venue: String,
    val totalTickets: Int,
    val availableTickets: Int
)