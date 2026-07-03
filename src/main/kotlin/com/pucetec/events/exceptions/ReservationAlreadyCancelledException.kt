package com.pucetec.events.exceptions

/**
 * Se lanza si se intenta cancelar una reserva que ya fue cancelada.
 * Retorna HTTP 409.
 */
class ReservationAlreadyCancelledException(message: String =
"La reserva ya se encuentra cancelada") : RuntimeException(message)