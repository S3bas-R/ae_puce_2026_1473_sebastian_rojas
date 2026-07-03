package com.pucetec.events.exceptions

/**
 * ExceptionResponse: Molde de datos para enviar errores estructurados al cliente.
 */
data class ExceptionResponse(
    val message: String?,
    val source: String
)