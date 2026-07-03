package com.pucetec.events.exceptions

/**
 * Se lanza si un asistente ya cuenta con 4 reservas activas.
 * Retorna HTTP 409.
 */
class ReservationLimitExceededException(message: String =
"El asistente ha superado el límite máximo de 4 reservas activas") : RuntimeException(message)