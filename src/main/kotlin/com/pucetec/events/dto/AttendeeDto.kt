package com.pucetec.events.dto

/**
 * Lo que viaja desde el cliente para registrar un nuevo asistente.
 */
data class AttendeeRequest(
    val name: String,
    val email: String
)

/***
 * La informacion del asistente que devolvemos al exterior
 */
data class AttendeeResponse(
    val id: Long,
    val name: String,
    val email: String
)
